import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;

class DemoServer {

    public static void main(String[] args) throws Exception {

        Server server = ServerBuilder.forPort(6565)
                .addService(new GreeterImpl())
                // Commands for connection check: grpcurl -plaintext localhost:6565 list
                .addService(ProtoReflectionService.newInstance())
                .build();

        System.out.println("Starting server...");
        server.start();
        System.out.println("Server started!");
        server.awaitTermination();;
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
            HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}