package io.left.hellomesh;

import android.app.Activity;
import android.app.AlertDialog;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.lang.Object;

import io.left.rightmesh.android.AndroidMeshManager;
import io.left.rightmesh.android.MeshService;
import io.left.rightmesh.id.MeshID;
import io.left.rightmesh.mesh.MeshManager;
import io.left.rightmesh.mesh.MeshStateListener;
import io.left.rightmesh.util.MeshUtility;
import io.left.rightmesh.util.RightMeshException;
import io.reactivex.functions.Consumer;

import static io.left.rightmesh.mesh.MeshManager.DATA_RECEIVED;
import static io.left.rightmesh.mesh.MeshManager.PEER_CHANGED;
import static io.left.rightmesh.mesh.MeshManager.REMOVED;


/**
 * forked from https://github.com/RightMesh/HelloMesh
 *
 */

public class MainActivity extends Activity implements MeshStateListener {
    // Port to bind app to.
    private static final int HELLO_PORT = 1050;

    private String username = "Narwal";

    // MeshManager instance - interface to the mesh network.
    AndroidMeshManager mm = null;

    // Set to keep track of peers connected to the mesh.
    HashSet<MeshID> users = new HashSet<>();

    Button mButton;
    EditText mEdit;
    String lastmes;
    ScrollView cont;
    TextView userBox;
//    TextView messageView;

    /**
     * Called when app first opens, initializes {@link AndroidMeshManager} reference (which will
     * start the {@link MeshService} if it isn't already running.
     *
     * @param savedInstanceState passed from operating system
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = (Button) findViewById(R.id.Send_Button);
        mEdit = (EditText) findViewById(R.id.input_message);
        lastmes = ((TextView) findViewById(R.id.last_recieved_message)).getText().toString();
        cont = ((ScrollView) findViewById(R.id.message_container));
        userBox = (TextView) findViewById(R.id.UserList);


        mm = AndroidMeshManager.getInstance(MainActivity.this, MainActivity.this);
        mm.setPattern("chatroom");

        mEdit.setOnKeyListener(new View.OnKeyListener(){
            @Override
            public boolean onKey(View v, int kc, KeyEvent ke){
                if (ke.getAction()==KeyEvent.ACTION_DOWN&&kc==KeyEvent.KEYCODE_ENTER){
                    try {
                        sendMessage(v);
                        return true;
                    } catch (RightMeshException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });
        mEdit.setImeActionLabel("SEND", KeyEvent.KEYCODE_ENTER);

        changeUsername(userBox);




    }

    public void changeUsername(View v){
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_set_username, null);

        final EditText mUsername = (EditText) mView.findViewById(R.id.etUsername);
        Button mOK = (Button) mView.findViewById(R.id.mOK);

        mBuilder.setView(mView);
        final AlertDialog dialog = mBuilder.create();
        dialog.show();

        mOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!(mUsername.getText().toString().isEmpty())) {
                    Toast.makeText(MainActivity.this,
                            R.string.success_login_msg,
                            Toast.LENGTH_SHORT).show();
                    username = mUsername.getText().toString();
                    dialog.hide();

                } else {
                    Toast.makeText(MainActivity.this,
                            R.string.error_username_msg,
                            Toast.LENGTH_SHORT).show();

                }
            }
        });

        mUsername.setOnKeyListener(new View.OnKeyListener(){
            @Override
            public boolean onKey(View v, int kc, KeyEvent ke){
                if (ke.getAction()==KeyEvent.ACTION_DOWN&&kc==KeyEvent.KEYCODE_ENTER){
                    if (!(mUsername.getText().toString().isEmpty())) {
                        Toast.makeText(MainActivity.this,
                                R.string.success_login_msg,
                                Toast.LENGTH_SHORT).show();
                        username = mUsername.getText().toString();
                        dialog.hide();

                    } else {
                        Toast.makeText(MainActivity.this,
                                R.string.error_username_msg,
                                Toast.LENGTH_SHORT).show();

                    }
                }
                return false;
            }
        });
        mUsername.setImeActionLabel("OK", KeyEvent.KEYCODE_ENTER);
    }

    /**
     * Called when activity is on screen.
     */
    @Override
    protected void onResume() {
        try {
            super.onResume();
            mm.resume();
        } catch (MeshService.ServiceDisconnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the app is being closed (not just navigated away from). Shuts down
     * the {@link AndroidMeshManager} instance.
     */
    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            mm.stop();
        } catch (MeshService.ServiceDisconnectedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Called by the {@link MeshService} when the mesh state changes. Initializes mesh connection
     * on first call.
     *
     * @param uuid  our own user id on first detecting
     * @param state state which indicates SUCCESS or an error code
     */
    @Override
    public void meshStateChanged(MeshID uuid, int state) {
        if (state == MeshStateListener.SUCCESS) {
            try {
                // Binds this app to MESH_PORT.
                // This app will now receive all events generated on that port.
                mm.bind(HELLO_PORT);

                // Subscribes handlers to receive events from the mesh.
                mm.on(DATA_RECEIVED, new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {
                        handleDataReceived((MeshManager.RightMeshEvent) o);
                    }

                });
                mm.on(PEER_CHANGED, new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {
                        handlePeerChanged((MeshManager.RightMeshEvent) o);
                    }
                });


                // If you are using Java 8 or a lambda backport like RetroLambda, you can use
                // a more concise syntax, like the following:
                // mm.on(PEER_CHANGED, this::handlePeerChanged);
                // mm.on(DATA_RECEIVED, this::dataReceived);

                // Enable buttons now that mesh is connected.
                Button btnConfigure = (Button) findViewById(R.id.btnConfigure);
                btnConfigure.setEnabled(true);
            } catch (RightMeshException e) {
                String status = "Error initializing the library" + e.toString();
                Toast.makeText(getApplicationContext(), status, Toast.LENGTH_SHORT).show();
                TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
                txtStatus.setText(status);
                return;
            }
        }

        // Update display on successful calls (i.e. not FAILURE or DISABLED).
        if (state == MeshStateListener.SUCCESS || state == MeshStateListener.RESUME) {
            updateStatus();
        }
    }

    /**
     * Update the {@link TextView} with a list of all peers.
     */
    private void updateStatus() {
        String status = "My ID: " + mm.getUuid().toString() + "\n" + "\nPeers:\n";
        for (MeshID user : users) {
            status += user.toString() + "\n";
        }
        //TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
        //txtStatus.setText(status);
        userBox.setText(status);
        TextView messageView = (TextView) findViewById(R.id.last_recieved_message);
        messageView.setText(lastmes);
    }

    /**
     * Handles incoming data events from the mesh - toasts the contents of the data.
     *
     * @param e event object from mesh
     */
    private void handleDataReceived(MeshManager.RightMeshEvent e) {
        final MeshManager.DataReceivedEvent event = (MeshManager.DataReceivedEvent) e;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Toast data contents.
                String message = new String(event.data);
                // Play a notification.
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(MainActivity.this, notification);
                r.play();
                DateFormat df = new SimpleDateFormat("HH:mm:ss");
                Date d = new Date();
                lastmes += df.format(d) + ": " + message + "\n";
                cont.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateStatus();
            }
        });
    }

    /**
     * Handles peer update events from the mesh - maintains a list of peers and updates the display.
     *
     * @param e event object from mesh
     */
    private void handlePeerChanged(MeshManager.RightMeshEvent e) {
        // Update peer list.
        MeshManager.PeerChangedEvent event = (MeshManager.PeerChangedEvent) e;
        if (event.state != REMOVED && !users.contains(event.peerUuid)) {
            users.add(event.peerUuid);
        } else if (event.state == REMOVED) {
            users.remove(event.peerUuid);
        }

        DateFormat df = new SimpleDateFormat("HH:mm:ss");
        Date d = new Date();
//        lastmes += df.format(d) + ": " + event.peerUuid.toString() + " has Connected! \n";

        // Update display.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateStatus();
            }
        });
    }

    /**
     * Sends the message in mEdit to all known peers.
     *
     * @param v calling view
     */

    public void sendMessage(View v) throws RightMeshException {
        for (MeshID reciever : users) {
            String ms = username + ": " + mEdit.getText().toString();
            MeshUtility.Log(this.getClass().getCanonicalName(), "MSG: " + ms);
            byte[] testData = ms.getBytes();
            mm.sendDataReliable(reciever, HELLO_PORT, testData);
        }
        mEdit.setText("");

    }

    /**
     * Open mesh settings screen.
     *
     * @param v calling view
     */
    public void configure(View v) {
        try {
            mm.showSettingsActivity();
        } catch (RightMeshException ex) {
            MeshUtility.Log(this.getClass().getCanonicalName(), "Service not connected");
        }
    }

}

