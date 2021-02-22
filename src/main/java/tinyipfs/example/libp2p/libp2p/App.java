package tinyipfs.example.libp2p.libp2p;

import org.apache.tuweni.bytes.Bytes;
import tinyipfs.example.libp2p.discovery.*;

/**
 * description: App <br>
 *
 * @author xie hui <br>
 * @version 1.0 <br>
 * @date 2020/9/9 16:19 <br>
 */
public class App {
    public static void main(String[] args) {
        Libp2pNetwork network = new Libp2pNetwork();
        network.start();

        System.out.println("network.getAddress()"+network.getAddress());
//        network.connect(peerAddress);
//        PeerAddress peerAddress = network.createPeerAddress("");

        //dial 的连接格式不一样 是 network.getAddress() + host.getPeerId
        //eg /ip4/192.168.3.5/tcp/11112/ipfs/16Uiu2HAm3NZUwzzNHfnnB8ADfnuP5MTDuqjRb3nTRBxPTQ4g7Wjj
        network.connect1("/ip4/192.168.3.5/tcp/11111/ipfs/16Uiu2HAm3NZUwzzNHfnnB8ADfnuP5MTDuqjRb3nTRBxPTQ4g7Wjj");

    }
}
