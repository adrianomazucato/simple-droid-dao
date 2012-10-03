package eu.janmuller.android.dao.api;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * Coder: Jan Müller
 * Date: 03.10.12
 * Time: 15:17
 */
public class UUIDId extends StringId {

    public UUIDId(String id) {
        super(id);
    }

    public UUIDId() {

        super(getUUID());
    }

    private static String getUUID() {

        return UUID.randomUUID().toString();
    }
}
