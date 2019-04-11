package mobileapp.ctemplar.com.ctemplarapp.repository;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import mobileapp.ctemplar.com.ctemplarapp.repository.entity.MessageEntity;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM messages")
    List<MessageEntity> getAll();

    @Query("SELECT * FROM messages WHERE folderName=:folderName or (:folderName='inbox' AND showInInbox = 1) ORDER BY updated DESC")
    List<MessageEntity> getAllByFolder(String folderName);

    @Insert(onConflict = REPLACE)
    void save(MessageEntity messageEntity);

    @Insert(onConflict = REPLACE)
    void saveAll(List<MessageEntity> mailboxes);

    @Delete
    void delete(MessageEntity mailbox);

    @Query("DELETE FROM messages")
    void deleteAll();

    @Query("SELECT * FROM messages WHERE id= :id")
    MessageEntity getById(long id);

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :id")
    void updateIsStarred(long id, boolean isStarred);

    @Query("UPDATE messages SET isRead = :isRead WHERE id = :id")
    void updateIsRead(long id, boolean isRead);

    @Query("DELETE FROM messages WHERE folderName=:folderName")
    void deleteAllByFolder(String folderName);

    @Query("DELETE FROM messages WHERE parent=:id")
    void deleteAllByParentId(String id);

    @Query("SELECT * FROM messages WHERE parent=:id")
    List<MessageEntity> getByParentId(String id);

    @Query("UPDATE messages SET folderName=:newFolderName WHERE id=:messageId")
    void updateFolderName(long messageId, String newFolderName);
}
