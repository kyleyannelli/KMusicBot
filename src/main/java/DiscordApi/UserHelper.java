package DiscordApi;

import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class UserHelper {
    public static boolean isAdmin(Optional<Server> server, User user) {
        AtomicBoolean isAdmin = new AtomicBoolean(false);
        if(user.isBotOwner()) return true;
        server.ifPresent(presentServer -> {
            presentServer.getHighestRole(user).ifPresent(role -> {
                for(PermissionType permission : role.getPermissions().getAllowedPermission()) {
                    if(permission.equals(PermissionType.ADMINISTRATOR)) {
                        isAdmin.set(true);
                        break;
                    }
                }
            });
        });
        return isAdmin.get();
    }
}
