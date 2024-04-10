/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RunnableWrapper<T> implements Runnable {

	private final Runnable runnable;

	private final AtomicReference<T> contextRef;

	private final Supplier<T> contextGetter;

	private final Consumer<T> contextSetter;

	public RunnableWrapper(Runnable runnable, Supplier<T> contextGetter, Consumer<T> contextSetter) {
		assert null != runnable && null != contextGetter && null != contextSetter;
		this.runnable = runnable;
		this.contextGetter = contextGetter;
		this.contextSetter = contextSetter;
		contextRef = new AtomicReference<>(contextGetter.get());
	}

	@Override
	public void run() {
		// preserve
		T latestContext = contextGetter.get();
		// set context
		T contextValue = contextRef.get();
		contextSetter.accept(contextValue);
		try {
			runnable.run();
		}
		finally {
			// restore
			contextSetter.accept(latestContext);
		}
	}
}
