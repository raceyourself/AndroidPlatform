package com.raceyourself.raceyourself.game;

import android.bluetooth.BluetoothDevice;

import com.raceyourself.platform.bluetooth.BluetoothHelper;
import com.raceyourself.platform.bluetooth.BluetoothListener;
import com.raceyourself.raceyourself.game.event_listeners.GameEventListener;
import com.raceyourself.raceyourself.game.event_listeners.RegularUpdateListener;
import com.raceyourself.raceyourself.game.position_controllers.PositionController;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

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

    // the callback that broadcasts data to glass. Called by the gameService at regular intervals.
    private RegularUpdateListener regularUpdateListener = new RegularUpdateListener() {
        @Override
        public void onRegularUpdate() {

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

                broadcast(message.toString());
            }

        }
    }.setRecurrenceInterval(BROADCAST_INTERVAL);

    private GameEventListener gameEventListener = new GameEventListener() {
        @Override
        public void onGameEvent(String eventTag) {
            if (eventTag.equals("Finished")) {
                JSONObject message = new JSONObject();
                try {
                    message.put("action", "finish_race");
                } catch (JSONException e) {
                    log.error("Error creating finish_race JSON object to send to glass");
                    return;
                }
                broadcast(message.toString());
            } else if (eventTag.equals("Paused")) {
                JSONObject message = new JSONObject();
                try {
                    message.put("action", "pause");
                    message.put("value", true);
                } catch (JSONException e) {
                    log.error("Error creating pause_race JSON object to send to glass");
                    return;
                }
                broadcast(message.toString());
            } else if (eventTag.equals("Resumed")) {
                JSONObject message = new JSONObject();
                try {
                    message.put("action", "pause");
                    message.put("value", false);
                } catch (JSONException e) {
                    log.error("Error creating resume_race JSON object to send to glass");
                    return;
                }
                broadcast(message.toString());
            }
        }
    };

    public GlassController() {
        bluetoothHelper.registerListener(this);
        bluetoothHelper.startBluetoothClient();
    }

    public synchronized void setGameService(GameService gs) {
        // the first time gs is set, add a listener
        if (gameService == null && gs != null) {
            gs.registerRegularUpdateListener(regularUpdateListener);
            gs.registerGameEventListener(gameEventListener);
        }
        this.gameService = gs;
        if (gameService == null) {
            // likely finished race, send a finished message
            JSONObject message = new JSONObject();
            try {
                message.put("action", "quit_race");
            } catch (JSONException e) {
                log.error("Error creating JSON object to send to glass");
                return;
            }
            broadcast(message.toString());
        }
    }

    public boolean isConnected() {
        if (bluetoothHelper.getBluetoothPeers().length > 0) {
            return true;
        } else {
            return false;
        }
    }

    private void broadcast(String message) {
        if (isConnected()) {
            log.debug("Broadcasting bluetooth message to glass: " + message.toString());
            bluetoothHelper.broadcast(message.toString());
        } else {
            log.debug("Not broadcasting to glass as not connected. Message was: " + message.toString());
        }
    }

    @Override
    public void onConnected(BluetoothDevice device) {
        log.info("Connected to bluetooth device: " + device.getName());
    }

    @Override
    public void onDisconnected(BluetoothDevice device) {
        log.info("Disconnected from bluetooth device: " + device.getName());
    }

    @Override
    public void onMessageReceived(String message) {
        log.info("BluetoothHelper message received: " + message);
        try {
            JSONObject jsonObject = new JSONObject(new JSONTokener(message));
            String action = jsonObject.getString("action");

            // reply to ping messages
            if (action.equals("set_ping")) {
                log.debug("Received ping message from glass: " + message.toString());
                double value = jsonObject.getLong("value");
                JSONObject response = new JSONObject();
                response.put("action", "set_ping");
                response.put("value", value);
                broadcast(response.toString());
            }

            // action pause command
            else if (action.equals("pause")) {
                log.debug("Received pause message from glass: " + message.toString());
                boolean value = jsonObject.getBoolean("value");
                if (value) {
                    gameService.stop();
                } else {
                    gameService.start();
                }
            }

            // action quit command
            else if (action.equals("quit")) {
                // do nothing for now
                log.debug("Received quit message from glass, ignoring: " + message.toString());
            }

            // log unrecognised messages
            else {
                log.debug("Unrecognised bluetooth message from glass: " + message.toString());
            }

        } catch (JSONException e) {
            log.error("Error handling bluetooth message from glass", e);
        }


    }

}
