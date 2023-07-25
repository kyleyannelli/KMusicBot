package DiscordApi;

import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @deprecated This class will no longer be maintained.
 */
@Deprecated
public class UserHelper {
    public static boolean isAdmin(Server server, User user) {
        AtomicBoolean isAdmin = new AtomicBoolean(false);
        if(user.isBotOwner()) return true;
        server.getHighestRole(user).ifPresent(role -> {
            for (PermissionType permission : role.getPermissions().getAllowedPermission()) {
                if (permission.equals(PermissionType.ADMINISTRATOR)) {
                    isAdmin.set(true);
                    break;
                }
            }
        });
        return isAdmin.get();
    }
}
