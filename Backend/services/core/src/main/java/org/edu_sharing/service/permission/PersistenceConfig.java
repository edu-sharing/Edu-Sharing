package org.edu_sharing.service.permission;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(value = "org.edu_sharing", annotationClass = Mapper.class)
public class PersistenceConfig {

}
