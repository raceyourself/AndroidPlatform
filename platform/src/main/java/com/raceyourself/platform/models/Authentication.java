package com.raceyourself.platform.models;

import static com.roscopeco.ormdroid.Query.eql;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Entity;

/**
 * Authentication permissions for a provider (facebook, twitter, google+).
 * States which actions can be taken through a provider.
 * 
 * Consistency model: Client can indirectly effect permissions by authorizing the server. 
 *                    Server can replace collection.
 */
public class Authentication extends Entity {

    @JsonIgnore
    public int id;
    public String provider;
    public String permissions;

    public Authentication() {
    }

    public static Authentication getAuthenticationByProvider(String provider) {
        return query(Authentication.class).where(eql("provider", provider))
                .execute();
    }

    public static List<Authentication> getAuthentications() {
        return query(Authentication.class).executeMulti();
    }

    public String getProvider() {
        return provider;
    }

    public Set<String> getPermissions() {
        Set<String> set = new HashSet<String>();
        String[] perms = permissions.split(",");
        for (String permission : perms) {
            set.add(permission);
        }
        return set;
    }

    public boolean hasPermissions(String permissions) {
        String[] perms = permissions.split(",");
        return hasPermissions(perms);
    }

    public boolean hasPermissions(String... perms) {
        Set<String> permissions = getPermissions();
        for (String permission : perms) {
            if (!permissions.contains(permission))
                return false;
        }
        return true;
    }
}
