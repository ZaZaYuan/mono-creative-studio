package dev.studio.atelier;

import java.util.List;
import org.apache.ibatis.annotations.*;
import dev.studio.atelier.Models.*;

@Mapper
public interface Repository {
  @Select("SELECT * FROM templates ORDER BY id") List<Template> templates();
  @Select("SELECT * FROM templates WHERE id=#{id}") Template template(String id);
  @Insert("INSERT INTO tasks(id,owner_id,template_id,status,current_stage,fail_at_stage,created_at,updated_at) VALUES(#{id},#{ownerId},#{templateId},#{status},#{currentStage},#{failAtStage},#{createdAt},#{updatedAt})") void insertTask(Task task);
  @Select("SELECT * FROM tasks WHERE id=#{id}") Task task(String id);
  @Select("SELECT * FROM tasks WHERE id=#{id} AND owner_id=#{ownerId}") Task ownedTask(@Param("id") String id,@Param("ownerId") String ownerId);
  @Select("SELECT * FROM tasks WHERE owner_id=#{ownerId} ORDER BY created_at DESC") List<Task> tasks(String ownerId);
  @Update("UPDATE tasks SET status=#{status}, current_stage=#{stage}, failure_reason=#{reason}, result_file_id=#{resultFileId}, updated_at=NOW(3) WHERE id=#{id}")
  void updateTask(@Param("id") String id,@Param("status") String status,@Param("stage") String stage,@Param("reason") String reason,@Param("resultFileId") String resultFileId);
  @Insert("INSERT INTO files(id,task_id,kind,original_name,mime_type,storage_name,created_at) VALUES(#{id},#{taskId},#{kind},#{originalName},#{mimeType},#{storageName},#{createdAt})") void insertFile(StoredFile file);
  @Select("SELECT * FROM files WHERE id=#{id}") StoredFile file(String id);
  @Select("SELECT * FROM files WHERE task_id=#{taskId} AND kind='INPUT' ORDER BY created_at") List<StoredFile> inputFiles(String taskId);
}
