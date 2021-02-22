package tinyipfs.example.libp2p.libp2p;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
@Slf4j
public class XdagBlockHandler extends ByteToMessageCodec<byte[]> {
//    @Override
//    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
//        System.out.println("encode");
//        String msg = "this is encode";
//        byte[] bytes = msg.getBytes(CharsetUtil.UTF_8);
//        byteBuf.writeBytes(bytes);
//        channelHandlerContext.writeAndFlush(byteBuf);
//    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) {
        log.debug("XdagBlockHandler readableBytes decode" + in.readableBytes() + " bytes");
        out.add(convertByteBufToString(in));
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, byte[] bytes, ByteBuf byteBuf) throws Exception {
        log.info("XdagBlockHandler encode");
        byteBuf.writeBytes(bytes);
        String msg = "A message";
        byte[] a = msg.getBytes(CharsetUtil.UTF_8);
        ByteBuf buf = Unpooled.wrappedBuffer(a);
        channelHandlerContext.writeAndFlush(buf);
        channelHandlerContext.pipeline().writeAndFlush(buf);
    }

    public String convertByteBufToString(ByteBuf buf) {
        String str;
        if(buf.hasArray()) { // 处理堆缓冲区
            str = new String(buf.array(), buf.arrayOffset() + buf.readerIndex(), buf.readableBytes());
        } else { // 处理直接缓冲区以及复合缓冲区
            byte[] bytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), bytes);
            str = new String(bytes, 0, buf.readableBytes());
        }
        return str;
    }


//    @Override
//    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
//        System.out.println("encode");
//    }
}
