/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.dataservices;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.JailData;
import io.github.nucleuspowered.nucleus.api.data.LocationData;
import io.github.nucleuspowered.nucleus.api.data.MuteData;
import io.github.nucleuspowered.nucleus.api.data.NoteData;
import io.github.nucleuspowered.nucleus.api.data.NucleusUser;
import io.github.nucleuspowered.nucleus.api.data.WarnData;
import io.github.nucleuspowered.nucleus.api.data.mail.MailData;
import io.github.nucleuspowered.nucleus.api.exceptions.NoSuchWorldException;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.configurate.datatypes.UserDataNode;
import io.github.nucleuspowered.nucleus.dataservices.dataproviders.DataProvider;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.modules.message.commands.SocialSpyCommand;
import io.github.nucleuspowered.nucleus.modules.nickname.config.NicknameConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import uk.co.drnaylor.quickstart.exceptions.IncorrectAdapterTypeException;
import uk.co.drnaylor.quickstart.exceptions.NoModuleException;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UserService extends Service<UserDataNode>
        implements NucleusUser {

    private final NucleusPlugin plugin;
    private final UUID uuid;
    private final Instant serviceLoadTime = Instant.now();

    // Use to keep hold of whether this is the first time on the server for a player.
    private boolean firstPlay = false;

    // Use for /back.
    private Transform<World> lastLocation = null;
    private boolean logLastLocation = true;

    // Use for /staffchat
    private boolean inStaffChat = false;

    // Used as a cache.
    private UserStorageService uss;
    private Text nickname = null;

    @NonnullByDefault
    public UserService(NucleusPlugin plugin, DataProvider<UserDataNode> provider, User user) throws Exception {
        this(plugin, provider, user.getUniqueId());
    }

    @NonnullByDefault
    public UserService(NucleusPlugin plugin, DataProvider<UserDataNode> provider, UUID uuid) throws Exception {
        super(provider);
        data = provider.load();
        Preconditions.checkNotNull("uuid", uuid );
        Preconditions.checkNotNull("plugin", plugin);
        this.plugin = plugin;
        this.uuid = uuid;
    }

    @Override
    public User getUser() {
        if (uss == null) {
            uss = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
        }

        return uss.get(this.uuid).orElseGet(() -> uss.getOrCreate(GameProfile.of(this.uuid, null)));
    }

    public Optional<MuteData> getMuteData() {
        return Optional.ofNullable(data.getMuteData());
    }

    public void setMuteData(MuteData mData) {
        data.setMuteData(mData);
    }

    public void removeMuteData() {
        data.setMuteData(null);
    }

    @Override
    public List<WarnData> getWarnings() {
        return ImmutableList.copyOf(data.getWarnings());
    }

    public void setWarnings(List<WarnData> warnings) {
        data.setWarnings(warnings);
    }

    @Override
    public void addWarning(WarnData warning) {
        List<WarnData> warnings = data.getWarnings();
        if (warnings == null) {
            warnings = Lists.newArrayList();
        }

        warnings.add(warning);
        warnings.sort((x, y) -> Boolean.compare(x.isExpired(), y.isExpired()));
        data.setWarnings(warnings);
    }

    @Override
    public boolean removeWarning(WarnData warning) {
        List<WarnData> warnings = data.getWarnings();
        if (warnings.removeIf(x -> x.getEndTimestamp().equals(warning.getEndTimestamp()) &&
                x.getReason().equals(warning.getReason()) &&
                x.getTimeFromNextLogin().equals(warning.getTimeFromNextLogin()) &&
                x.getWarner().equals(warning.getWarner()) &&
                x.getDate().equals(warning.getDate()))) {
            data.setWarnings(warnings);
            return true;
        }

        return false;
    }

    @Override
    public boolean clearWarnings() {
        if (!data.getWarnings().isEmpty()) {
            data.setWarnings(Lists.newArrayList());
            return true;
        } else {
            return false;
        }
    }

    public List<NoteData> getNotes() {
        return ImmutableList.copyOf(data.getNotes());
    }

    public void addNote(NoteData note) {
        List<NoteData> notes = data.getNotes();
        if (notes == null) {
            notes = Lists.newArrayList();
        }

        notes.add(note);
        data.setNotes(notes);
    }

    public boolean removeNote(NoteData note) {
        List<NoteData> notes = data.getNotes();
        if (notes.removeIf(x -> x.getNoter().equals(note.getNoter()) && x.getNote().equals(note.getNote()))) {
            data.setNotes(notes);
            return true;
        }

        return false;
    }

    public boolean clearNotes() {
        if (!data.getNotes().isEmpty()) {
            data.setNotes(Lists.newArrayList());
            return true;
        } else {
            return false;
        }
    }

    public boolean isSocialSpy() {
        // Only a spy if they have the permission!
        return data.isSocialspy() && plugin.getPermissionRegistry().getService(SocialSpyCommand.class).testBase(getUser());
    }

    public boolean setSocialSpy(boolean socialSpy) {
        data.setSocialspy(socialSpy);

        // Permission checks! Return true if it's what we wanted.
        return isSocialSpy() == socialSpy;
    }

    @Override
    public boolean isInvulnerable() {
        return data.isInvulnerable();
    }

    @Override
    public void setInvulnerable(boolean invuln) {
        data.setInvulnerable(invuln);
    }

    @Override
    public boolean isFlying() {
        User user = getUser();
        if (user.isOnline()) {
            data.setFly(user.getPlayer().get().get(Keys.CAN_FLY).orElse(false));
        }

        return data.isFly();
    }

    @Override
    public void setFlying(boolean fly) {
        data.setFly(fly);
    }

    @Override
    public Optional<JailData> getJailData() {
        return Optional.ofNullable(data.getJailData());
    }

    @Override
    public Optional<Instant> getLastLogin() {
        if (data.getLogin() == 0) {
            return Optional.empty();
        }

        return Optional.of(Instant.ofEpochMilli(data.getLogin()));
    }

    public void setLastLogin(Instant login) {
        data.setLogin(login.toEpochMilli());
    }

    @Override
    public Optional<Instant> getLastLogout() {
        if (data.getLogout() == 0) {
            return Optional.empty();
        }

        return Optional.of(Instant.ofEpochMilli(data.getLogout()));
    }

    @Override
    public Optional<LocationData> getHome(String home) {
        if (data.getHomeData() == null) {
            return Optional.empty();
        }

        LocationNode ln = Util.getValueIgnoreCase(data.getHomeData(), home).orElse(null);
        if (ln != null) {
            try {
                return Optional.of(new LocationData(home, ln.getLocation(), ln.getRotation()));
            } catch (NoSuchWorldException e) {
                return Optional.of(new LocationData(home, null, null));
            }
        }

        return Optional.empty();
    }

    @Override
    public Map<String, LocationData> getHomes() {
        if (data.getHomeData() == null || data.getHomeData().isEmpty()) {
            return Maps.newHashMap();
        }

        return data.getHomeData().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, x -> {
                try {
                    return new LocationData(x.getKey(), x.getValue().getLocation(), x.getValue().getRotation());
                } catch (NoSuchWorldException e) {
                    return new LocationData(x.getKey(), null, null);
                }
            }));
    }

    @Override
    public boolean setHome(String home, Location<World> location, Vector3d rotation) {
        return setHome(home, location, rotation, false);
    }

    @Override
    public boolean setHome(String home, Location<World> location, Vector3d rotation, boolean overwrite) {
        final Pattern warpName = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{1,15}$");

        Map<String, LocationNode> homeData = data.getHomeData();
        if (homeData == null) {
            homeData = Maps.newHashMap();
        }

        Optional<String> os = Util.getKeyIgnoreCase(data.getHomeData(), home);
        if (os.isPresent() || !warpName.matcher(home).matches()) {
            if (!overwrite || !deleteHome(home)) {
                return false;
            }
        }

        homeData.put(home, new LocationNode(location, rotation));
        data.setHomeData(homeData);
        return true;
    }

    @Override
    public boolean deleteHome(String home) {
        Map<String, LocationNode> homeData = data.getHomeData();
        if (homeData == null) {
            return false;
        }

        Optional<String> os = Util.getKeyIgnoreCase(data.getHomeData(), home);
        if (os.isPresent()) {
            homeData.remove(os.get());
            data.setHomeData(homeData);
            return true;
        }

        return false;
    }

    @Override
    public boolean isTeleportToggled() {
        return data.isTeleportToggled();
    }

    @Override
    public void setTeleportToggled(boolean toggle) {
        data.setTeleportToggled(toggle);
    }

    @Override
    public Optional<Text> getNicknameWithPrefix() {
        if (getNicknameAsText().isPresent()) {
            String p = getNickPrefix();
            if (p == null || p.isEmpty()) {
                return getNicknameAsText();
            }

            return Optional.of(Text.join(TextSerializers.FORMATTING_CODE.deserialize(p), getNicknameAsText().get()));
        }

        return Optional.empty();
    }

    @Override
    public Optional<Text> getNicknameAsText() {
        if (this.nickname != null) {
            return Optional.of(this.nickname);
        }

        Optional<String> os = getNicknameAsString();
        if (!os.isPresent()) {
            return Optional.empty();
        }

        nickname = TextSerializers.FORMATTING_CODE.deserialize(os.get());
        return Optional.of(nickname);
    }

    @Override
    public Optional<String> getNicknameAsString() {
        return Optional.ofNullable(data.getNickname());
    }

    @Override
    public void setNickname(String nickname) {
        data.setNickname(nickname);
        this.nickname = null;
        String p = getNickPrefix();
        if (p != null && !p.isEmpty()) {
            nickname = p + nickname;
        }

        Text nick = TextSerializers.FORMATTING_CODE.deserialize(nickname);
        getUser().getPlayer().ifPresent(x -> x.offer(Keys.DISPLAY_NAME, nick));
    }

    @Override
    public void removeNickname() {
        nickname = null;
        getUser().remove(Keys.DISPLAY_NAME);
        data.setNickname(null);
    }

    public List<MailData> getMail() {
        return ImmutableList.copyOf(data.getMailDataList());
    }

    public void addMail(MailData mailData) {
        List<MailData> mailDataList = data.getMailDataList();
        if (mailDataList == null) {
            mailDataList = Lists.newArrayList();
        }

        mailDataList.add(mailData);
        data.setMailDataList(mailDataList);
    }

    public boolean removeMail(MailData mailData) {
        List<MailData> lmd = data.getMailDataList();
        if (lmd.removeIf(x -> x.getDate().equals(mailData.getDate()) &&
                x.getMessage().equals(mailData.getMessage()) && x.getUuid().equals(mailData.getUuid()))) {
            data.setMailDataList(lmd);
            return true;
        }

        return false;
    }

    public boolean clearMail() {
        if (!data.getMailDataList().isEmpty()) {
            data.setMailDataList(Lists.newArrayList());
            return true;
        } else {
            return false;
        }
    }

    public boolean isFlyingSafe() {
        return data.isFly();
    }

    public void setJailData(JailData jdata) {
        data.setJailData(jdata);
    }

    public void removeJailData() {
        setJailData(null);
    }

    public void setOnLogout(Location<World> location) {
        setLastLogout(Instant.now());
        data.setLastLocation(new LocationNode(location));

        // Set data based toggles.
        isFlying();
        isInvulnerable();
    }

    public Optional<Location<World>> getLocationOnLogin() {
        LocationNode ln = data.getLocationOnLogin();
        if (ln == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(ln.getLocation());
        } catch (NoSuchWorldException e) {
            return Optional.empty();
        }
    }

    public void sendToLocationOnLogin(Location<World> worldLocation) {
        data.setLocationOnLogin(new LocationNode(worldLocation));
    }

    public void removeLocationOnLogin() {
        data.setLocationOnLogin(null);
    }

    public void setLastLogout(Instant logout) {
        data.setLogout(logout.toEpochMilli());
    }

    @Override
    public UUID getUniqueID() {
        return getUser().getUniqueId();
    }

    public boolean jailOnNextLogin() {
        return data.isJailOffline();
    }

    public void setJailOnNextLogin(boolean set) {
        data.setJailOffline(!getUser().isOnline() && set);
    }

    public Map<String, Instant> getKitLastUsedTime() {
        final Map<String, Instant> r = Maps.newHashMap();
        data.getKitLastUsedTime().forEach((k, v) -> r.put(k.toLowerCase(), Instant.ofEpochSecond(v)));
        return r;
    }

    public void addKitLastUsedTime(String kitName, Instant lastTime) {
        Map<String, Long> kitLastUsedTime = data.getKitLastUsedTime();
        kitLastUsedTime.put(kitName.toLowerCase(), lastTime.getEpochSecond());
        data.setKitLastUsedTime(kitLastUsedTime);
    }

    public void removeKitLastUsedTime(String kitName) {
        Map<String, Long> kitLastUsedTime = data.getKitLastUsedTime();
        kitLastUsedTime.remove(kitName.toLowerCase());
        data.setKitLastUsedTime(kitLastUsedTime);
    }

    // -- Powertools
    public Map<String, List<String>> getPowertools() {
        return ImmutableMap.copyOf(data.getPowertools());
    }

    @Override
    public Optional<List<String>> getPowertoolForItem(ItemType item) {
        List<String> tools = data.getPowertools().get(item.getId());
        if (tools != null) {
            return Optional.of(ImmutableList.copyOf(tools));
        }

        return Optional.empty();
    }

    public void setPowertool(ItemType type, List<String> commands) {
        data.getPowertools().put(type.getId(), commands);
    }

    public void clearPowertool(ItemType type) {
        data.getPowertools().remove(type.getId());
    }

    public void clearPowertool(String type) {
        data.getPowertools().remove(type);
    }

    @Override
    public boolean isPowertoolToggled() {
        return data.isPowertoolToggle();
    }

    @Override
    public void setPowertoolToggle(boolean set) {
        data.setPowertoolToggle(set);
    }

    @Override
    public List<UUID> getIgnoreList() {
        return ImmutableList.copyOf(data.getIgnoreList());
    }

    @Override
    public boolean addToIgnoreList(UUID uuid) {
        if (!data.getIgnoreList().contains(uuid)) {
            data.getIgnoreList().add(uuid);
            return true;
        }

        return false;
    }

    @Override
    public boolean removeFromIgnoreList(UUID uuid) {
        return data.getIgnoreList().remove(uuid);
    }

    @Override
    public boolean isFrozen() {
        return data.isFrozen();
    }

    @Override
    public void setFrozen(boolean value) {
        data.setFrozen(value);
    }

    public Optional<Transform<World>> getLastLocation() {
        return Optional.ofNullable(this.lastLocation);
    }

    public void setLastLocation(Transform<World> location) {
        this.lastLocation = location;
    }

    public boolean isLogLastLocation() {
        return logLastLocation;
    }

    public void setLogLastLocation(boolean logLastLocation) {
        this.logLastLocation = logLastLocation;
    }

    public boolean isInStaffChat() {
        return inStaffChat;
    }

    public void setInStaffChat(boolean inStaffChat) {
        this.inStaffChat = inStaffChat;
    }

    @Override
    public boolean isFirstPlay() {
        return firstPlay;
    }

    public void setFirstPlay(boolean firstPlay) {
        this.firstPlay = firstPlay;
    }

    public Optional<String> getLastIp() {
        return Optional.ofNullable(data.getIpaddress());
    }

    public void setLastIp(InetAddress address) {
        data.setIpaddress(address.toString());
    }

    /**
     * Primarily used internally to determine if the {@link UserDataManager#removeOfflinePlayers()} method should really remove this.
     *
     * @return The {@link Instant} the {@link UserService} was loaded.
     */
    public final Instant serviceLoadTime() {
        return serviceLoadTime;
    }

    public Optional<Location<World>> getLogoutLocation() {
        try {
            return Optional.ofNullable(data.getLastLocation().getLocation());
        } catch (NoSuchWorldException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private String getNickPrefix() {
        try {
            return plugin.getModuleContainer().getConfigAdapterForModule("nickname", NicknameConfigAdapter.class).getNodeOrDefault().getPrefix();
        } catch (NoModuleException | IncorrectAdapterTypeException e) {
            return null;
        }
    }

    public boolean isCommandSpy() {
        return data.isCommandSpy();
    }

    public void setCommandSpy(boolean set) {
        data.setCommandSpy(set);
    }
}
