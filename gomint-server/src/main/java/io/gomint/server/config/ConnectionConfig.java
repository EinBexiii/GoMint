package io.gomint.server.config;

import com.blackypaw.simpleconfig.SimpleConfig;
import com.blackypaw.simpleconfig.annotation.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author geNAZt
 * @version 1.0
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ConnectionConfig extends SimpleConfig {

    @Comment("Enable connection encryption. It uses AES-128 so it costs a bit of CPU power")
    private boolean enableEncryption = true;

    @Comment("Level of compression used for packets. Lower = less CPU / higher traffic; Higher = more CPU / lower traffic")
    private int compressionLevel = 7;

}
