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

package com.tencent.polaris.plugins.ai.connector;

import com.tencent.polaris.api.plugin.skill.SkillDownloadRequest;
import com.tencent.polaris.api.plugin.skill.SkillDownloadResponse;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.api.plugin.skill.SkillGetResponse;
import com.tencent.polaris.api.plugin.skill.SkillListRequest;
import com.tencent.polaris.api.plugin.skill.SkillListResponse;
import com.tencent.polaris.api.plugin.skill.SkillResource;
import com.tencent.polaris.api.plugin.skill.SkillResourceVersion;
import com.tencent.polaris.api.plugin.skill.SkillStorageInfo;
import com.tencent.polaris.api.plugin.skill.SkillVersionInfo;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.specification.api.v1.skill.manage.PolarisSkillGRPCService;
import com.tencent.polaris.specification.api.v1.skill.manage.SkillProto;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Convert between Skill POJOs and protobuf messages.
 */
final class SkillProtoConverter {

    private SkillProtoConverter() {
    }

    static PolarisSkillGRPCService.GetSkillRequest toGetRequest(SkillGetRequest request) {
        PolarisSkillGRPCService.GetSkillRequest.Builder builder = PolarisSkillGRPCService.GetSkillRequest.newBuilder();
        if (StringUtils.isNotBlank(request.getName())) {
            builder.setName(request.getName());
        }
        if (StringUtils.isNotBlank(request.getNamespace())) {
            builder.setNamespace(request.getNamespace());
        }
        if (StringUtils.isNotBlank(request.getVersion())) {
            builder.setVersion(request.getVersion());
        }
        return builder.build();
    }

    static PolarisSkillGRPCService.ListSkillsRequest toListRequest(SkillListRequest request) {
        PolarisSkillGRPCService.ListSkillsRequest.Builder builder = PolarisSkillGRPCService.ListSkillsRequest.newBuilder();
        fillListRequest(builder, request);
        return builder.build();
    }

    static PolarisSkillGRPCService.DownloadSkillRequest toDownloadRequest(SkillDownloadRequest request) {
        PolarisSkillGRPCService.DownloadSkillRequest.Builder builder = PolarisSkillGRPCService.DownloadSkillRequest.newBuilder();
        if (StringUtils.isNotBlank(request.getName())) {
            builder.setName(request.getName());
        }
        if (StringUtils.isNotBlank(request.getNamespace())) {
            builder.setNamespace(request.getNamespace());
        }
        if (StringUtils.isNotBlank(request.getVersion())) {
            builder.setVersion(request.getVersion());
        }
        if (StringUtils.isNotBlank(request.getTag())) {
            builder.setTag(request.getTag());
        }
        if (StringUtils.isNotBlank(request.getFormat())) {
            builder.setFormat(request.getFormat());
        }
        return builder.build();
    }

    static SkillGetResponse toGetResponse(PolarisSkillGRPCService.GetSkillResponse proto) {
        SkillGetResponse response = new SkillGetResponse();
        response.setCode(proto.getCode());
        response.setInfo(proto.getInfo());
        response.setContent(proto.getContent());
        response.setResource(toResource(proto.getResource()));
        response.setResourceVersion(toResourceVersion(proto.getVersion()));
        return response;
    }

    static SkillListResponse toListResponse(PolarisSkillGRPCService.ListSkillsResponse proto) {
        SkillListResponse response = new SkillListResponse();
        response.setCode(proto.getCode());
        response.setInfo(proto.getInfo());
        response.setTotal(proto.getTotal());
        List<SkillResource> resources = new ArrayList<>();
        for (SkillProto.SkillResource item : proto.getResourcesList()) {
            resources.add(toResource(item));
        }
        response.setResources(resources);
        return response;
    }

    static SkillDownloadResponse assembleDownload(Iterator<PolarisSkillGRPCService.DownloadSkillResponse> iterator) {
        SkillDownloadResponse response = new SkillDownloadResponse();
        ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();
        boolean firstFrame = true;
        while (iterator.hasNext()) {
            PolarisSkillGRPCService.DownloadSkillResponse frame = iterator.next();
            firstFrame = fillDownloadFrame(response, zipBuffer, frame, firstFrame);
        }
        if (zipBuffer.size() > 0) {
            response.setZipContent(zipBuffer.toByteArray());
        }
        return response;
    }

    private static void fillListRequest(PolarisSkillGRPCService.ListSkillsRequest.Builder builder,
            SkillListRequest request) {
        if (StringUtils.isNotBlank(request.getNamespace())) {
            builder.setNamespace(request.getNamespace());
        }
        if (StringUtils.isNotBlank(request.getName())) {
            builder.setName(request.getName());
        }
        if (StringUtils.isNotBlank(request.getTag())) {
            builder.setTag(request.getTag());
        }
        if (StringUtils.isNotBlank(request.getOwner())) {
            builder.setOwner(request.getOwner());
        }
        if (StringUtils.isNotBlank(request.getScope())) {
            builder.setScope(request.getScope());
        }
        if (StringUtils.isNotBlank(request.getKeyword())) {
            builder.setKeyword(request.getKeyword());
        }
        if (StringUtils.isNotBlank(request.getOrderBy())) {
            builder.setOrderBy(request.getOrderBy());
        }
        if (StringUtils.isNotBlank(request.getOrderType())) {
            builder.setOrderType(request.getOrderType());
        }
        builder.setOffset(request.getOffset());
        builder.setLimit(request.getLimit());
    }

    private static boolean fillDownloadFrame(SkillDownloadResponse response, ByteArrayOutputStream zipBuffer,
            PolarisSkillGRPCService.DownloadSkillResponse frame, boolean firstFrame) {
        boolean stillFirst = firstFrame;
        if (firstFrame) {
            response.setCode(frame.getCode());
            response.setInfo(frame.getInfo());
            response.setVersion(frame.getVersion());
            response.setContentType(frame.getContentType());
            response.setTotalSize(frame.getTotalSize());
            response.setFilename(frame.getFilename());
            stillFirst = false;
        }
        if (StringUtils.isNotBlank(frame.getContent())) {
            response.setContent(frame.getContent());
        }
        if (!frame.getZipChunk().isEmpty()) {
            zipBuffer.write(frame.getZipChunk().toByteArray(), 0, frame.getZipChunk().size());
        }
        return stillFirst;
    }

    private static SkillResource toResource(SkillProto.SkillResource proto) {
        SkillResource resource = new SkillResource();
        resource.setId(proto.getId());
        resource.setName(proto.getName());
        resource.setNamespace(proto.getNamespace());
        resource.setDescription(proto.getDescription());
        resource.setStatus(proto.getStatus());
        resource.setTags(proto.getTagsList());
        resource.setExamples(proto.getExamplesList());
        resource.setExt(proto.getExtMap());
        resource.setSource(proto.getSource());
        resource.setMetaVersion(proto.getMetaVersion());
        resource.setScope(proto.getScope());
        resource.setOwner(proto.getOwner());
        resource.setDownloadCount(proto.getDownloadCount());
        resource.setCreateTime(proto.getCreateTime());
        resource.setModifyTime(proto.getModifyTime());
        resource.setVersionInfo(toVersionInfo(proto.getVersionInfo()));
        return resource;
    }

    private static SkillVersionInfo toVersionInfo(SkillProto.VersionInfo proto) {
        SkillVersionInfo info = new SkillVersionInfo();
        info.setActiveVersion(proto.getActiveVersion());
        info.setLatestVersion(proto.getLatestVersion());
        info.setTotalVersions(proto.getTotalVersions());
        return info;
    }

    private static SkillResourceVersion toResourceVersion(SkillProto.SkillResourceVersion proto) {
        SkillResourceVersion version = new SkillResourceVersion();
        version.setId(proto.getId());
        version.setName(proto.getName());
        version.setNamespace(proto.getNamespace());
        version.setVersion(proto.getVersion());
        version.setAuthor(proto.getAuthor());
        version.setDescription(proto.getDescription());
        version.setStatus(proto.getStatus());
        version.setDownloadCount(proto.getDownloadCount());
        version.setCreateTime(proto.getCreateTime());
        version.setModifyTime(proto.getModifyTime());
        version.setStorage(toStorage(proto.getStorage()));
        return version;
    }

    private static SkillStorageInfo toStorage(SkillProto.StorageInfo proto) {
        SkillStorageInfo storage = new SkillStorageInfo();
        storage.setProvider(proto.getProvider());
        storage.setFiles(proto.getFilesList());
        storage.setScope(proto.getScope());
        storage.setContentDigest(proto.getContentDigest());
        return storage;
    }
}
