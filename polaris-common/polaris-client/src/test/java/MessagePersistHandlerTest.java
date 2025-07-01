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


import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MessagePersistHandlerTest.java
 *
 * @author andrewshan
 * @date 2019/9/6
 */
public class MessagePersistHandlerTest {

    //    @Test
    public void testLoadPersistedServices() {
        String regexPattern = "^svc#.+#.+\\.json$";
        Pattern compilePattern = Pattern.compile(regexPattern);
        Matcher matcher = compilePattern.matcher("svc#nsa#svc1.json");
        System.out.println("result is " + matcher.matches());
        byte[] encodeBytes = Base64.getEncoder().encode("a.b".getBytes());
        System.out.println("encode for a.b is " + new String(encodeBytes));
        String persistDirPath = "E:\\cl5\\task";
        Path curDir = Paths.get(persistDirPath);
        try {
            Files.walkFileTree(curDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                    Path fileName = filePath.getFileName();
                    FileTime fileTime = attrs.creationTime();
                    System.out.println("file name is " + fileName + ", fileTime is " + fileTime);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}