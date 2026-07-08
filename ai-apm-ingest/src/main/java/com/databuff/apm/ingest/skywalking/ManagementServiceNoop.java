package com.databuff.apm.ingest.skywalking;

import io.grpc.stub.StreamObserver;
import org.apache.skywalking.apm.network.common.v3.Commands;
import org.apache.skywalking.apm.network.management.v3.InstancePingPkg;
import org.apache.skywalking.apm.network.management.v3.InstanceProperties;
import org.apache.skywalking.apm.network.management.v3.ManagementServiceGrpc;

/** No-op Management handler so Java Agent startup does not see UNIMPLEMENTED. */
public final class ManagementServiceNoop extends ManagementServiceGrpc.ManagementServiceImplBase {

    private static final Commands EMPTY = Commands.getDefaultInstance();

    @Override
    public void reportInstanceProperties(InstanceProperties request, StreamObserver<Commands> responseObserver) {
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }

    @Override
    public void keepAlive(InstancePingPkg request, StreamObserver<Commands> responseObserver) {
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }
}
