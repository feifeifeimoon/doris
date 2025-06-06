// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
// This file is copied from
// https://github.com/ClickHouse/ClickHouse/blob/master/src/Functions/Plus.cpp
// and modified by Doris

#include <utility>

#include "runtime/decimalv2_value.h"
#include "vec/common/arithmetic_overflow.h"
#include "vec/data_types/number_traits.h"
#include "vec/functions/function_binary_arithmetic.h"
#include "vec/functions/simple_function_factory.h"

namespace doris::vectorized {

template <PrimitiveType TypeA, PrimitiveType TypeB>
struct PlusImpl {
    using A = typename PrimitiveTypeTraits<TypeA>::CppNativeType;
    using B = typename PrimitiveTypeTraits<TypeB>::CppNativeType;
    static constexpr PrimitiveType ResultType =
            NumberTraits::ResultOfAdditionMultiplication<A, B>::Type;
    static const constexpr bool allow_decimal = true;

    template <PrimitiveType Result = ResultType>
    static inline typename PrimitiveTypeTraits<Result>::CppNativeType apply(A a, B b) {
        /// Next everywhere, static_cast - so that there is no wrong result in expressions of the form Int64 c = UInt32(a) * Int32(-1).
        return static_cast<typename PrimitiveTypeTraits<Result>::CppNativeType>(a) + b;
    }

    template <typename Result = DecimalV2Value>
    static inline DecimalV2Value apply(DecimalV2Value a, DecimalV2Value b) {
        return DecimalV2Value(a.value() + b.value());
    }

    /// Apply operation and check overflow. It's used for Decimal operations. @returns true if overflowed, false otherwise.
    template <PrimitiveType Result = ResultType>
    static inline bool apply(A a, B b, typename PrimitiveTypeTraits<Result>::CppNativeType& c) {
        return common::add_overflow(
                static_cast<typename PrimitiveTypeTraits<Result>::CppNativeType>(a), b, c);
    }
};

struct NamePlus {
    static constexpr auto name = "add";
};
using FunctionPlus = FunctionBinaryArithmetic<PlusImpl, NamePlus, false>;

void register_function_plus(SimpleFunctionFactory& factory) {
    factory.register_function<FunctionPlus>();
}
} // namespace doris::vectorized
