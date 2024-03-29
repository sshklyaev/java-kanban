package service;

import Server.KVTaskClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.EpicTask;
import model.SubTask;
import model.Task;


import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class HttpTaskManager extends FileBackedTasksManager {

    private Gson GsonUtilClass;
    private Gson gson = new GsonBuilder().registerTypeAdapter(HttpTaskManager.class, new GsonInstant()).create();

    private final KVTaskClient taskClient;


    public HttpTaskManager(String  url) throws ManagerSaveException {
        super(null);
        try {
            taskClient = new KVTaskClient(url);
        } catch (IOException | InterruptedException e) {
            throw new ManagerSaveException("Ошибка при подключении к KVServer");
        }
    }

    @Override
    public void save() throws ManagerSaveException {
        try {
            taskClient.put("CommonTasks", gson.toJson(new ArrayList<>(mapOfDataTask.values())));
            taskClient.put("Epics", gson.toJson(new ArrayList<>(mapOfDataEpicTask.values())));
            taskClient.put("Subtasks", gson.toJson(new ArrayList<>(mapOfDataEpicTask.values())));
            taskClient.put("history", gson.toJson(historyManager.getHistory().stream().
                    map(Task::getTaskId).collect(Collectors.toList())));

        } catch (IOException | InterruptedException err) {
            throw new ManagerSaveException("Ошибка при сохранении данных");
        }
    }

    public void load() {
        Map<Integer, Task> tasks = gson.fromJson(
                taskClient.load("tasks"),
                new TypeToken<HashMap<Integer, Task>>() {
                }.getType()
        );
        Map<Integer, EpicTask> epics = gson.fromJson(
                taskClient.load("epics"),
                new TypeToken<HashMap<Integer, EpicTask>>() {
                }.getType()
        );
        Map<Integer, SubTask> subtasks = gson.fromJson(
                taskClient.load("subtasks"),
                new TypeToken<HashMap<Integer, SubTask>>() {
                }.getType()
        );
        List<Task> historyList = gson.fromJson(
                taskClient.load("history"),
                new TypeToken<List<Task>>() {
                }.getType()
        );
        HistoryManager history = new InMemoryHistoryManager();
        historyList.forEach(history::addLast);

        int startId = Integer.parseInt(taskClient.load("startId"));

        this.mapOfDataTask = (HashMap<Integer, Task>) tasks;
        this.mapOfDataEpicTask= (HashMap<Integer, EpicTask>) epics;
        this.mapOfDataSubTask = (HashMap<Integer, SubTask>) subtasks;
        this.historyManager = history;
        this.prioritizedTaskSet.addAll(tasks.values());
        this.prioritizedTaskSet.addAll(epics.values());
        this.prioritizedTaskSet.addAll(subtasks.values());
    }
}
