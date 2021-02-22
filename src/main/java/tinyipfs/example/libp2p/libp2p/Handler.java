package tinyipfs.example.libp2p.libp2p;

import io.libp2p.core.Connection;
import io.libp2p.core.P2PChannel;
import io.libp2p.core.Stream;
import io.libp2p.core.multistream.ProtocolBinding;
import io.libp2p.core.multistream.ProtocolDescriptor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import tinyipfs.example.libp2p.discovery.LibP2PNodeId;
import tinyipfs.example.libp2p.discovery.NodeId;
import tinyipfs.util.SafeFuture;

import java.util.concurrent.CompletableFuture;


@Slf4j
@ChannelHandler.Sharable
public class Handler implements ProtocolBinding<Handler.Controller> {

    private String announce;
    private Controller controller;
    final CompletableFuture<Handler> activeFuture = new CompletableFuture<>();
    ChannelHandlerContext ctx;

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public Handler(String announce, Controller controller) {
        this.announce = announce;
        this.controller = controller;
    }

    public Handler() {
    }

    @NotNull
    @Override
    public ProtocolDescriptor getProtocolDescriptor() {
        return  new ProtocolDescriptor("xdagj");
    }
//teku

    @NotNull
    @Override
    public SafeFuture<Controller> initChannel(@NotNull P2PChannel p2PChannel, @NotNull String s) {
        System.out.println("initChannel212121");
//        final Connection connection = ((Stream) p2PChannel).getConnection();
//        final NodeId nodeId = new LibP2PNodeId(connection.secureSession().getRemoteId());

        final Connection connection = ((io.libp2p.core.Stream) p2PChannel).getConnection();
        final NodeId nodeId = new LibP2PNodeId(connection.secureSession().getRemoteId());
        Controller controller = new Controller(nodeId, p2PChannel);
        XdagBlockHandler xdagBlockHandler = new XdagBlockHandler();
        if (!p2PChannel.isInitiator()) {
            System.out.println("fial");
        }
//        p2PChannel.pushHandler(xdagBlockHandler);
        p2PChannel.pushHandler(controller);
        return controller.activeFuture;
    }



    //Xdag03
     static class Controller extends SimpleChannelInboundHandler<ByteBuf>{

        final NodeId nodeid;
        final P2PChannel p2pChannel;
        protected final SafeFuture<Controller> activeFuture = new SafeFuture<>();

        public Controller(NodeId nodeid, P2PChannel p2pChannel) {
            this.nodeid = nodeid;
            this.p2pChannel = p2pChannel;
        }



        @Override
        public void channelActive(ChannelHandlerContext ctx) {

            System.out.println("channelActive");
            String msg = "A message";
            byte[] bytes = msg.getBytes(CharsetUtil.UTF_8);
            final ByteBuf reqByteBuf = ctx.alloc().buffer();
            reqByteBuf.writeBytes(bytes);
            ctx.channel().writeAndFlush(reqByteBuf);
            activeFuture.complete(this);
        }


        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf o) {
            System.out.println("channelRead0");
            String s = o.toString();
            System.out.println(s);
        }
    }
}
