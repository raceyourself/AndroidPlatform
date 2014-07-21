package com.raceyourself.raceyourself.game;

import android.bluetooth.BluetoothDevice;

import com.raceyourself.platform.bluetooth.BluetoothHelper;
import com.raceyourself.platform.bluetooth.BluetoothListener;
import com.raceyourself.raceyourself.game.event_listeners.RegularUpdateListener;
import com.raceyourself.raceyourself.game.position_controllers.PositionController;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by benlister on 15/07/2014.
 */
@Slf4j
public class GlassController implements BluetoothListener {

    private final long BROADCAST_INTERVAL = 1000l;
    @Getter private GameService gameService;
    private BluetoothHelper bluetoothHelper = new BluetoothHelper();
    @Getter private boolean connected = false;

    // the callback that broadcasts data to glass. Called by the gameService at regular intervals.
    private RegularUpdateListener regularUpdateListener = new RegularUpdateListener() {
        @Override
        public void onRegularUpdate() {

            if (!isConnected()) {
                log.debug("Not broadcasting to glass as not connected.");
                return;
            }

            synchronized (GlassController.this) {

                JSONObject message = new JSONObject();

                try {

                    // top-level action to perform
                    message.put("action", "position_update");

                    // add player data to message
                    JSONObject playerData = new JSONObject();
                    PositionController player = gameService.getLocalPlayer();
                    playerData.put("distance", player.getRealDistance());
                    playerData.put("elapsed_time", player.getElapsedTime());
                    playerData.put("current_speed", player.getCurrentSpeed());
                    playerData.put("average_speed", player.getAverageSpeed());
                    playerData.put("ahead_behind", player.getRealDistance() - gameService.getLeadingOpponent().getRealDistance());
                    playerData.put("calories", player.getRealDistance() * 75.0 * 1.2 / 1000.0);  // dist * weight(kg) * factor / 1000
                    message.put("player_data", playerData);

                    // add opponent data to message - may have multiple opponents
                    for (PositionController p : gameService.getPositionControllers()) {
                        if (p.isLocalPlayer()) continue;  // opponents only
                        JSONObject opponentData = new JSONObject();
                        opponentData.put("distance", p.getRealDistance());
                        opponentData.put("elapsed_time", p.getElapsedTime());
                        opponentData.put("current_speed", p.getCurrentSpeed());
                        opponentData.put("average_speed", p.getAverageSpeed());
                        opponentData.put("ahead_behind", p.getRealDistance() - player.getRealDistance());
                        opponentData.put("calories", p.getRealDistance() * 75.0 * 1.2 / 1000.0);
                        message.put("opponent_data", playerData);
                    }

                } catch (JSONException e) {
                    log.error("Error creating JSON object to send to glass");
                    return;
                }

                log.debug("Broadcasting position update to glass: " + message.toString());
                bluetoothHelper.broadcast(message.toString());
            }

        }
    }.setRecurrenceInterval(BROADCAST_INTERVAL);

    public GlassController() {
        bluetoothHelper.registerListener(this);
        bluetoothHelper.startBluetoothClient();
    }

    public synchronized void setGameService(GameService gs) {
        // the first time gs is set, add a listener
        if (gameService == null && gs != null) {
            gs.registerRegularUpdateListener(regularUpdateListener);
        }
        this.gameService = gs;
        if (gameService == null) {
            // likely finished race, send a finished message
            JSONObject message = new JSONObject();
            try {
                message.put("action", "position_update");
            } catch (JSONException e) {
                log.error("Error creating JSON object to send to glass");
                return;
            }
            log.debug("Broadcasting race finished message to glass: " + message.toString());
            bluetoothHelper.broadcast(message.toString());
        }
    }

    @Override
    public void onConnected(BluetoothDevice device) {
        this.connected = true;
        log.info("Connected to bluetoothHelper device: " + device.getName());
    }

    @Override
    public void onDisconnected(BluetoothDevice device) {
        this.connected = false;
        log.info("Disconnected from bluetoothHelper device: " + device.getName());
    }

    @Override
    public void onMessageReceived(String message) {
        log.info("BluetoothHelper message received: " + message);
    }

}
