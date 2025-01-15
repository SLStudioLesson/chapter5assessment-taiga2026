package com.taskapp.logic;

import com.taskapp.dataaccess.LogDataAccess;
import com.taskapp.dataaccess.TaskDataAccess;
import com.taskapp.dataaccess.UserDataAccess;
import com.taskapp.exception.AppException;
import com.taskapp.model.Log;
import com.taskapp.model.Task;
import com.taskapp.model.User;

import java.time.LocalDate;
import java.util.List;

public class TaskLogic {
    private final TaskDataAccess taskDataAccess;
    private final LogDataAccess logDataAccess;
    private final UserDataAccess userDataAccess;


    public TaskLogic() {
        taskDataAccess = new TaskDataAccess();
        logDataAccess = new LogDataAccess();
        userDataAccess = new UserDataAccess();
    }

    /**
     * 自動採点用に必要なコンストラクタのため、皆さんはこのコンストラクタを利用・削除はしないでください
     * @param taskDataAccess
     * @param logDataAccess
     * @param userDataAccess
     */
    public TaskLogic(TaskDataAccess taskDataAccess, LogDataAccess logDataAccess, UserDataAccess userDataAccess) {
        this.taskDataAccess = taskDataAccess;
        this.logDataAccess = logDataAccess;
        this.userDataAccess = userDataAccess;
    }

    /**
     * 全てのタスクを表示します。
     *
     * @see com.taskapp.dataaccess.TaskDataAccess#findAll()
     * @param loginUser ログインユーザー
     */
    public void showAll(User loginUser) {
        List<Task> tasks = taskDataAccess.findAll(); // すべてのタスクを取得
    
        tasks.forEach(task -> {
            String status = "";
            switch (task.getStatus()) {
                case 0: status = "未着手"; break;
                case 1: status = "着手中"; break;
                case 2: status = "完了"; break;
                default: status = "不明なステータス"; break;
            }
    
            // 担当者を表示する
            User assignedUser = task.getRepUser();
            String assignedUserName = assignedUser != null ? assignedUser.getName() : "不明な担当者";
    
            if (assignedUser != null && assignedUser.getCode() == loginUser.getCode()) {
                assignedUserName = "あなたが担当しています";
            } else {
                assignedUserName = assignedUser != null ? assignedUser.getName() + "が担当しています" : "不明な担当者が担当しています";
            }
    
            // タスク情報を表示
            System.out.println("タスク名：" + task.getName() + ", 担当者名：" + assignedUserName + ", ステータス：" + status);
        });
    }

    /**
     * 新しいタスクを保存します。
     *
     * @see com.taskapp.dataaccess.UserDataAccess#findByCode(int)
     * @see com.taskapp.dataaccess.TaskDataAccess#save(com.taskapp.model.Task)
     * @see com.taskapp.dataaccess.LogDataAccess#save(com.taskapp.model.Log)
     * @param code タスクコード
     * @param name タスク名
     * @param repUserCode 担当ユーザーコード
     * @param loginUser ログインユーザー
     * @throws AppException ユーザーコードが存在しない場合にスローされます
     */
    public void save(int code, String name, int repUserCode, User loginUser) throws AppException {
        // 同じタスクコードのタスクがすでに存在しないか確認
        Task existingTask = taskDataAccess.findByCode(code);  // findByCodeメソッドを使う
        if (existingTask != null) {
            throw new AppException("指定されたタスクコードはすでに存在します");
        }

        // 担当ユーザーが存在するかを確認
        User repUser = userDataAccess.findByCode(repUserCode);
        if (repUser == null) {
            throw new AppException("存在するユーザーコードを入力してください");
        }

        // 新しいタスクを作成（ステータスは0: 未着手）
        Task newTask = new Task(code, name, 0, repUser);

        // 新しいタスクを保存
        taskDataAccess.save(newTask);

        // タスクの登録に伴うログを作成
        Log log = new Log(code, loginUser.getCode(), 0, LocalDate.now());
        logDataAccess.save(log);

        // 登録完了メッセージを表示
        System.out.println(name + "の登録が完了しました。");
    }

    /**
     * タスクのステータスを変更します。
     *
     * @see com.taskapp.dataaccess.TaskDataAccess#findByCode(int)
     * @see com.taskapp.dataaccess.TaskDataAccess#update(com.taskapp.model.Task)
     * @see com.taskapp.dataaccess.LogDataAccess#save(com.taskapp.model.Log)
     * @param code タスクコード
     * @param status 新しいステータス
     * @param loginUser ログインユーザー
     * @throws AppException タスクコードが存在しない、またはステータスが前のステータスより1つ先でない場合にスローされます
     */
    public void changeStatus(int code, int status, User loginUser) throws AppException {
        // タスクコードに対応するタスクを取得
        Task task = taskDataAccess.findByCode(code);
        
        // タスクが存在しない場合
        if (task == null) {
            throw new AppException("存在するタスクコードを入力してください");
        }
    
        // 現在のステータスを取得
        int currentStatus = task.getStatus();
        
        // ステータス変更が前のステータスより1つ先かどうかを確認
        if (currentStatus == 0) {
            // 現在のステータスが「未着手（0）」の場合
            if (status != 1 && status != 2) {
                throw new AppException("ステータスは、前のステータスより1つ先のもののみを選択してください");
            }
        } else if (currentStatus == 1) {
            // 現在のステータスが「着手中（1）」の場合
            if (status != 2) {
                throw new AppException("ステータスは、前のステータスより1つ先のもののみを選択してください");
            }
        } else if (currentStatus == 2) {
            // 現在のステータスが「完了（2）」の場合
            throw new AppException("完了したタスクはステータスを変更できません");
        }
    
        // ステータスの変更
        task.setStatus(status);
        
        // タスクを更新
        taskDataAccess.update(task);
        
        // ステータス変更のログを記録
        Log log = new Log(code, loginUser.getCode(), status, LocalDate.now());
        logDataAccess.save(log);
        
        System.out.println("ステータスの変更が完了しました。");
    }

    /**
     * タスクを削除します。
     *
     * @see com.taskapp.dataaccess.TaskDataAccess#findByCode(int)
     * @see com.taskapp.dataaccess.TaskDataAccess#delete(int)
     * @see com.taskapp.dataaccess.LogDataAccess#deleteByTaskCode(int)
     * @param code タスクコード
     * @throws AppException タスクコードが存在しない、またはタスクのステータスが完了でない場合にスローされます
     */
    // public void delete(int code) throws AppException {
    // }
}