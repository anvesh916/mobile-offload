package com.nebuxe.mobileoffloading.utilities;

import com.google.android.gms.nearby.connection.Payload;
import com.nebuxe.mobileoffloading.pojos.TPayload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PayloadDataTransformer {

    public static Payload toPayload(TPayload tPayload) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(tPayload);
        objectOutputStream.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();

        Payload payload = Payload.fromBytes(bytes);
        return payload;
    }

    public static TPayload fromPayload(Payload payload) throws IOException, ClassNotFoundException {
        byte[] receivedBytes = payload.asBytes();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(receivedBytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

        return (TPayload) objectInputStream.readObject();
    }
}
