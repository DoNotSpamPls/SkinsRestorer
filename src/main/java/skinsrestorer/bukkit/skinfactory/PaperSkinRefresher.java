package skinsrestorer.bukkit.skinfactory;

import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.entity.Player;
import skinsrestorer.shared.utils.ReflectionUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

final class PaperSkinRefresher implements Consumer<Player> {
    private static final MethodHandle MH_REFRESH;

    @Override
    @SneakyThrows
    public void accept(Player player) {
        MH_REFRESH.invoke(player);
    }

    static {
        try {
            val field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            field.setAccessible(true);
            MethodHandles.publicLookup();
            MH_REFRESH = ((MethodHandles.Lookup)field.get(null)).findVirtual(ReflectionUtil.getBukkitClass("entity.CraftPlayer"), "refreshPlayer", MethodType.methodType(Void.TYPE));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
