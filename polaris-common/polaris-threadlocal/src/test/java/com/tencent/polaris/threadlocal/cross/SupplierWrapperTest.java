/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.threadlocal.cross;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class SupplierWrapperTest {

	private static final ThreadLocal<String> TEST_THREAD_LOCAL = new ThreadLocal<>();
	private static final String TEST = "TEST";

	@Test
	public void testGet() {
		TEST_THREAD_LOCAL.set(TEST);
		AtomicReference<Boolean> result = new AtomicReference<>(false);
		SupplierWrapper<Void, String> supplierWrapper = new SupplierWrapper<>(
				() -> {
					result.set(TEST.equals(TEST_THREAD_LOCAL.get()));
					return null;
				},
				TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);
		CompletableFuture.supplyAsync(supplierWrapper);
	}
}
