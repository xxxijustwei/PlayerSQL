package com.mengcraft.playersql.peer;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.SneakyThrows;

import java.util.Objects;
import java.util.UUID;

public abstract class IPacket {

    private final Protocol protocol;

    public IPacket(Protocol protocol) {
        this.protocol = Objects.requireNonNull(protocol);
    }

    public Protocol getProtocol() {
        return protocol;
    }

    protected abstract void read(ByteArrayDataOutput buf);

    @SneakyThrows
    public byte[] encode() {
        ByteArrayDataOutput buf = ByteStreams.newDataOutput();
        buf.writeByte(protocol.ordinal());
        read(buf);
        return buf.toByteArray();
    }

    public static IPacket decode(byte[] input) {
        ByteArrayDataInput buf = ByteStreams.newDataInput(input);
        Protocol protocol = Protocol.values()[buf.readByte()];
        return protocol.decode(buf);
    }

    public enum Protocol {

        PEER_READY {
            public IPacket decode(ByteArrayDataInput input) {
                PeerReady pk = new PeerReady();
                pk.setId(new UUID(input.readLong(), input.readLong()));
                return pk;
            }
        },

        DATA_BUF {
            public IPacket decode(ByteArrayDataInput input) {
                DataBuf pk = new DataBuf();
                pk.setId(new UUID(input.readLong(), input.readLong()));
                byte[] buf = new byte[input.readInt()];
                input.readFully(buf);
                pk.setBuf(buf);
                return pk;
            }
        },

        DATA_REQUEST {
            public IPacket decode(ByteArrayDataInput input) {
                DataRequest pk = new DataRequest();
                pk.setId(new UUID(input.readLong(), input.readLong()));
                return pk;
            }
        };

        public static final String TAG = "playersql@channel";

        Protocol() {
        }

        public IPacket decode(ByteArrayDataInput input) {
            throw new AbstractMethodError("decode");
        }
    }
}
