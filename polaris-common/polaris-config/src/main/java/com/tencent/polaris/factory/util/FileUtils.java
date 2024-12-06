/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.factory.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

/**
 * @author rod.xu
 * @date 2022/9/29 10:44 上午
 */
public class FileUtils {

	/**
	 * 校验文件夹是否能够读写
	 *
	 * @param dirPath 目录
	 * @throws IOException
	 */
	public static void dirPathCheck(String dirPath) throws IOException {
		File dirPathFile = new File(dirPath);
		try {
			if (!dirPathFile.exists() && !dirPathFile.mkdirs()) {
				throw new IOException(String.format("fail to create dir %s", dirPathFile));
			}
			//检查文件夹是否具备写权限
			if (!Files.isWritable(FileSystems.getDefault().getPath(dirPath))) {
				throw new IOException(String.format("fail to check permission for dir %s", dirPath));
			}
		}
		catch (Throwable e) {
			throw new IOException(String.format("fail to check permission for dir %s", dirPathFile), e);
		}
	}
}
