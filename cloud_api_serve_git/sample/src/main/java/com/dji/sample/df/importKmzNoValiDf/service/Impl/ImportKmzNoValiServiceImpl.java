package com.dji.sample.df.importKmzNoValiDf.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.df.importKmzNoValiDf.service.ImportKmzNoValiService;
import com.dji.sample.wayline.dao.IWaylineFileMapper;
import com.dji.sample.wayline.model.dto.KmzFileProperties;
import com.dji.sample.wayline.model.dto.WaylineFileDTO;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import com.dji.sdk.cloudapi.device.DeviceDomainEnum;
import com.dji.sdk.cloudapi.device.DeviceEnum;
import com.dji.sdk.cloudapi.device.DeviceSubTypeEnum;
import com.dji.sdk.cloudapi.device.DeviceTypeEnum;
import com.dji.sdk.cloudapi.wayline.WaylineTypeEnum;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.dji.sample.wayline.model.dto.KmzFileProperties.WAYLINE_FILE_SUFFIX;

@Service
@Transactional
public class ImportKmzNoValiServiceImpl implements ImportKmzNoValiService {
    @Autowired
    private IWaylineFileMapper mapper;

    @Autowired
    private OssServiceContext ossService;

    @Override
    public void importKmzFile(MultipartFile file, String workspaceId, String creator,Integer waylinePos) {
        Optional<WaylineFileDTO> waylineFileOpt = validKmzFile(file);
        if (waylineFileOpt.isEmpty()) {
            throw new RuntimeException("The file format is incorrect.");
        }
        try {
            WaylineFileDTO waylineFile = waylineFileOpt.get();
            waylineFile.setUsername(creator);
            ossService.putObject(OssConfiguration.bucket, waylineFile.getObjectKey(), file.getInputStream());
            this.saveWaylineFile(workspaceId, waylineFile,waylinePos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //按名称查询
    @Override
    public WaylineFileEntity getWaylineByFileName(String fileName) {
        WaylineFileEntity entity = mapper.selectOne(new LambdaQueryWrapper<WaylineFileEntity>().eq(WaylineFileEntity::getName,fileName));
        return entity;
    }

    //校验
    private Optional<WaylineFileDTO> validKmzFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (Objects.nonNull(filename) && !filename.endsWith(WAYLINE_FILE_SUFFIX)) {
            throw new RuntimeException("The file format is incorrect.");
        }
        try (ZipInputStream unzipFile = new ZipInputStream(file.getInputStream(), StandardCharsets.UTF_8)) {
            ZipEntry nextEntry = unzipFile.getNextEntry();
            while (Objects.nonNull(nextEntry)) {
                boolean isWaylines = (KmzFileProperties.FILE_DIR_FIRST + "/" + KmzFileProperties.FILE_DIR_SECOND_TEMPLATE).equals(nextEntry.getName());
                if (!isWaylines) {
                    nextEntry = unzipFile.getNextEntry();
                    continue;
                }
                SAXReader reader = new SAXReader();
                Document document = reader.read(unzipFile);
                if (!StandardCharsets.UTF_8.name().equals(document.getXMLEncoding())) {
                    throw new RuntimeException("The file encoding format is incorrect.");
                }
                Node droneNode = document.selectSingleNode("//" + KmzFileProperties.TAG_WPML_PREFIX + KmzFileProperties.TAG_DRONE_INFO);
                Node payloadNode = document.selectSingleNode("//" + KmzFileProperties.TAG_WPML_PREFIX + KmzFileProperties.TAG_PAYLOAD_INFO);
                if (Objects.isNull(droneNode) || Objects.isNull(payloadNode)) {
                    throw new RuntimeException("The file format is incorrect.");
                }
                DeviceTypeEnum type = DeviceTypeEnum.find(Integer.parseInt(droneNode.valueOf(KmzFileProperties.TAG_WPML_PREFIX + KmzFileProperties.TAG_DRONE_ENUM_VALUE)));
                DeviceSubTypeEnum subType = DeviceSubTypeEnum.find(Integer.parseInt(droneNode.valueOf(KmzFileProperties.TAG_WPML_PREFIX + KmzFileProperties.TAG_DRONE_SUB_ENUM_VALUE)));
                DeviceTypeEnum payloadType = DeviceTypeEnum.find(Integer.parseInt(payloadNode.valueOf(KmzFileProperties.TAG_WPML_PREFIX + KmzFileProperties.TAG_PAYLOAD_ENUM_VALUE)));
                DeviceSubTypeEnum payloadSubType = DeviceSubTypeEnum.find(0);
                String templateType = document.valueOf("//" + KmzFileProperties.TAG_WPML_PREFIX + KmzFileProperties.TAG_TEMPLATE_TYPE);

                return Optional.of(WaylineFileDTO.builder()
                        .droneModelKey(DeviceEnum.find(DeviceDomainEnum.DRONE, type, subType).getDevice())
                        .payloadModelKeys(List.of(DeviceEnum.find(DeviceDomainEnum.PAYLOAD, payloadType, payloadSubType).getDevice()))
                        .objectKey(OssConfiguration.objectDirPrefix + File.separator + filename)
                        .name(filename.substring(0, filename.lastIndexOf(WAYLINE_FILE_SUFFIX)))
                        .sign(DigestUtils.md5DigestAsHex(file.getInputStream()))
                        .templateTypes(List.of(WaylineTypeEnum.find(templateType).getValue()))
                        .build());
            }

        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    //存储数据库
    @Override
    public Integer saveWaylineFile(String workspaceId, WaylineFileDTO metadata,Integer waylinePos) {
        WaylineFileEntity file = this.dtoConvertToEntity(metadata);
        file.setWaylineId(UUID.randomUUID().toString());
        file.setWorkspaceId(workspaceId);
        file.setWaylinePos(waylinePos);
        if (!StringUtils.hasText(file.getSign())) {
            try (InputStream object = ossService.getObject(OssConfiguration.bucket, metadata.getObjectKey())) {
                if (object.available() == 0) {
                    throw new RuntimeException("The file " + metadata.getObjectKey() +
                            " does not exist in the bucket[" + OssConfiguration.bucket + "].");
                }
                file.setSign(DigestUtils.md5DigestAsHex(object));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int insertId = mapper.insert(file);
        return insertId > 0 ? file.getId() : insertId;
    }
    /**
     * Convert the received wayline object into a database entity object.
     * @param file
     * @return
     */
    private WaylineFileEntity dtoConvertToEntity(WaylineFileDTO file) {
        WaylineFileEntity.WaylineFileEntityBuilder builder = WaylineFileEntity.builder();

        if (file != null) {
            builder.droneModelKey(file.getDroneModelKey())
                    .name(file.getName())
                    .username(file.getUsername())
                    .objectKey(file.getObjectKey())
                    // Separate multiple payload data with ",".
                    .payloadModelKeys(String.join(",", file.getPayloadModelKeys()))
                    .templateTypes(file.getTemplateTypes().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(",")))
                    .favorited(file.getFavorited())
                    .sign(file.getSign())
                    .build();
        }

        return builder.build();
    }

}
