package com.radiofreederp.nodebbintegration.bukkit.commands;

import io.socket.client.Ack;
import com.radiofreederp.nodebbintegration.NodeBBIntegrationBukkit;
import com.radiofreederp.nodebbintegration.socketio.SocketIOClient;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Yari on 10/5/2015.
 */
public class CommandRegisterBukkit implements CommandExecutor
{
    private NodeBBIntegrationBukkit plugin;

    public CommandRegisterBukkit(NodeBBIntegrationBukkit _plugin) { plugin = _plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        // Sender needs to be a player.
        if (!(sender instanceof Player)) {
            sender.sendMessage("Command needs to be run by a player.");
            return true;
        }

        final CommandSender commandSender = sender;
        final String forumname;
        final String forumurl;

        // Get config
        try {
            forumname = plugin.getPluginConfig().getForumName();
            String u = plugin.getPluginConfig().getForumURL();
            String plain = ChatColor.stripColor(u);
            String last = plain.substring(plain.length()-1);
            if (!last.equals("/")) u += "/";
            forumurl = u;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }

        // Assert parameters.
        if (args.length != 1) {
            for (String str : plugin.getPluginConfig().getMessage("messages.register.AssertParameters")) {
                // TODO: Proper config parsing module.
                sender.sendMessage(p(str).replace("$1", forumurl));
            }
            return true;
        }

        // Trim key.
        if (args[0].length() > 4) args[0] = args[0].substring(4);

        // Alert
        for (String str : plugin.getPluginConfig().getMessage("messages.register.Alert")) {
            str = str.replaceAll("%forumname%", forumname);
            str = str.replaceAll("%forumurl%",forumurl);
            sender.sendMessage(p(str));
        }

        // If we're not connected, don't do anything.
        if (SocketIOClient.disconnected()) {
            for (String str : plugin.getPluginConfig().getMessage("messages.register.NotConnected")) {
                sender.sendMessage(p(str));
            }
            return true;
        }

        //
        JSONObject obj = new JSONObject();
        try {
            obj.put("key", plugin.getPluginConfig().getForumAPIKey());
            obj.put("id", ((Player) sender).getUniqueId().toString());
            obj.put("name", sender.getName());
            obj.put("pkey", args[0]);
        } catch (JSONException e) {
            e.printStackTrace();
            return true;
        }

        // DEBUG
        plugin.log("Sending commandRegister");

        SocketIOClient.emit("commandRegister", obj, new Ack() {
            @Override
            public void call(Object... args) {

                plugin.log("Received commandRegister callback");

                String result;
                String[] message;

                try {
                    if (args[0] != null) {
                        result = ((JSONObject)args[0]).getString("message");
                    }else{
                        result = ((JSONObject)args[1]).getString("result");
                    }
                }catch (Exception e) {
                    result = "BADRES";
                }

                RegisterResponse response = RegisterResponse.valueOf(result);

                if (response != null) {
                    message = response.getMessage();
                }
                else
                {
                    message = RegisterResponse.ERROR.getMessage();
                }

                for (String str : message) {
                    str = str.replaceAll("%forumname%", forumname);
                    str = str.replaceAll("%forumurl%",forumurl);
                    commandSender.sendMessage(p(str));
                }
            }
        });

        return true;
    }

    private enum RegisterResponse
    {
        REGISTER  ("messages.register.RegSuccess"),
        CREATE    ("messages.register.CreatedNewAccount"),
        FAILKEY   ("messages.register.FailKey"),
        FAILDB    ("messages.register.FailDB"),
        BADRES    ("messages.register.BadRes"),
        FAILDATA  ("messages.register.FailData"),
        ERROR     ("messages.register.ParsingError");

        private String key;
        private String[] message = null;

        RegisterResponse(String key) { this.key = key; }

        public String[] getMessage() {
            if (this.message == null) this.message = NodeBBIntegrationBukkit.instance.getPluginConfig().getMessage(this.key);
            return this.message;
        }
    }

    private String p(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}