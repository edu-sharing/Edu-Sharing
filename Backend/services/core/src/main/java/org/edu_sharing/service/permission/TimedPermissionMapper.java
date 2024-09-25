package org.edu_sharing.service.permission;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;


@Mapper
public interface TimedPermissionMapper {

    @Select("SELECT * FROM edu_timed_node_permission WHERE node_id = #{node_id}")
    List<TimedPermission> findAllByNodeId(@Param("node_id") String node_id);

    @Select("SELECT * FROM edu_timed_node_permission WHERE \"from\" <= #{date} AND activated = false")
    List<TimedPermission> findAllByFromAfterAndNotActivated(@Param("date") Date date);

    @Select("SELECT * FROM edu_timed_node_permission WHERE \"to\" < #{date}")
    List<TimedPermission> findAllByToBefore(@Param("date") Date date);

    @Insert("INSERT INTO edu_timed_node_permission(node_id,authority,permission,\"from\",\"to\", activated)VALUES(#{node_id},#{authority},#{permission},#{from},#{to}, #{activated}) ON CONFLICT (node_id, authority, permission) DO UPDATE SET \"from\" = #{from}, \"to\" = #{to}, activated = #{activated};")
    void save(TimedPermission timedPermission);

    @Delete("DELETE FROM edu_timed_node_permission WHERE node_id = #{node_id} AND authority = #{authority} AND permission = #{permission}")
    void delete(TimedPermission permission);

    @Delete("DELETE FROM edu_timed_node_permission WHERE node_id = #{node_id}")
    void deleteAllByNodeId(@Param("node_id") String node_id);

}
