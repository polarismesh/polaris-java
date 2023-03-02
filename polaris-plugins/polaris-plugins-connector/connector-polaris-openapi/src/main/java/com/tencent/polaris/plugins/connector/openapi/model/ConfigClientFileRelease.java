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

package com.tencent.polaris.plugins.connector.openapi.model;

import java.util.Date;

/**
 * @author fabian4 2023-03-02
 */
public class ConfigClientFileRelease {
        private String id;
        private String name;
        private String namespace;
        private String group;
        private String fileName;
        private String content;
        private String comment;
        private String md5;
        private String version;
        private Date createTime;
        private String createBy;
        private Date modifyTime;
        private String modifyBy;
        public void setId(String id) {
            this.id = id;
        }
        public String getId() {
            return id;
        }

        public void setName(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }
        public String getNamespace() {
            return namespace;
        }

        public void setGroup(String group) {
            this.group = group;
        }
        public String getGroup() {
            return group;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        public String getFileName() {
            return fileName;
        }

        public void setContent(String content) {
            this.content = content;
        }
        public String getContent() {
            return content;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
        public String getComment() {
            return comment;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }
        public String getMd5() {
            return md5;
        }

        public void setVersion(String version) {
            this.version = version;
        }
        public String getVersion() {
            return version;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }
        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateBy(String createBy) {
            this.createBy = createBy;
        }
        public String getCreateBy() {
            return createBy;
        }

        public void setModifyTime(Date modifyTime) {
            this.modifyTime = modifyTime;
        }
        public Date getModifyTime() {
            return modifyTime;
        }

        public void setModifyBy(String modifyBy) {
            this.modifyBy = modifyBy;
        }
        public String getModifyBy() {
            return modifyBy;
        }

    @Override
    public String toString() {
        return "ConfigClientRileRelease{" +
                "id='" + id + '\'' +
                ", name=" + name +
                ", namespace='" + namespace + '\'' +
                ", group='" + group + '\'' +
                ", fileName='" + fileName + '\'' +
                ", content=" + content +
                ", comment='" + comment + '\'' +
                ", md5='" + md5 + '\'' +
                ", version='" + version + '\'' +
                ", createTime=" + createTime +
                ", createBy='" + createBy + '\'' +
                ", modifyTime=" + modifyTime +
                ", modifyBy='" + modifyBy + '\'' +
                '}';
    }
}
