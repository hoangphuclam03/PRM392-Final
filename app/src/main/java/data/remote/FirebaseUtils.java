package data.remote;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseUtils {
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static FirebaseAuth getAuth() { return auth; }
    public static FirebaseFirestore getDatabase() { return db; }

    public static CollectionReference getUsersCollection() {
        return db.collection("Users");
    }

    public static CollectionReference getProjectsCollection() {
        return db.collection("Projects");
    }

    public static CollectionReference getTasksCollection() {
        return db.collection("Tasks");
    }
}
