package build.buildfarm.common.grpc;

import build.buildfarm.v1test.GrpcConfig;
import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GrpcChannelBuilder {
  private static final Logger logger = Logger.getLogger(GrpcChannelBuilder.class.getName());

  private static SslContext createSSlContext(String rootCert, String clientCert, String clientKey)
      throws IOException {
    SslContextBuilder sslContextBuilder;
    try {
      sslContextBuilder = GrpcSslContexts.forClient();
    } catch (Exception e) {
      String message = "Failed to init TLS infrastructure: " + e.getMessage();
      throw new IOException(message, e);
    }
    if (!rootCert.isEmpty()) {
      try {
        sslContextBuilder.trustManager(new File(rootCert));
      } catch (Exception e) {
        String message = "Failed to init TLS infrastructure using '%s' as root certificate: %s";
        message = String.format(message, rootCert, e.getMessage());
        throw new IOException(message, e);
      }
    }
    if (!clientCert.isEmpty() && !clientKey.isEmpty()) {
      try {
        sslContextBuilder.keyManager(new File(clientCert), new File(clientKey));
      } catch (Exception e) {
        String message = "Failed to init TLS infrastructure using '%s' as client certificate: %s";
        message = String.format(message, clientCert, e.getMessage());
        throw new IOException(message, e);
      }
    }
    try {
      return sslContextBuilder.build();
    } catch (Exception e) {
      String message = "Failed to init TLS infrastructure: " + e.getMessage();
      throw new IOException(message, e);
    }
  }

  public static ManagedChannel createChannel(GrpcConfig grpc_config) {
    NettyChannelBuilder builder = null;

    if (grpc_config.getTlsCertificate().isEmpty()) {
      builder =
          NettyChannelBuilder.forTarget(grpc_config.getTarget())
              .negotiationType(NegotiationType.PLAINTEXT);
    } else {
      builder =
          NettyChannelBuilder.forTarget(grpc_config.getTarget())
              .negotiationType(NegotiationType.TLS);

      SslContext sslContext = null;
      try {
        sslContext =
            createSSlContext(
                grpc_config.getTlsCertificate(),
                grpc_config.getTlsClientCertificate(),
                grpc_config.getTlsClientKey());
      } catch (IOException e) {
        logger.log(Level.SEVERE, "create ssl faled", e);
      }

      builder.sslContext(sslContext);
    }

    return builder.build();
  }
}
