package edu.purdue.jtk.ble.macos;

import com.sun.jna.Callback;
import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility wrapper around the Objective-C runtime and CoreFoundation,
 * accessed via JNA. All ObjC object references are carried as {@code long}
 * values representing native pointers.
 */
public final class ObjC {

    private static final NativeLibrary LIB_OBJC = NativeLibrary.getInstance("objc");
    private static final NativeLibrary LIB_CF = NativeLibrary.getInstance(
            "/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation");

    private static final Function F_GET_CLASS = LIB_OBJC.getFunction("objc_getClass");
    private static final Function F_SEL = LIB_OBJC.getFunction("sel_registerName");
    private static final Function F_ALLOC_PAIR = LIB_OBJC.getFunction("objc_allocateClassPair");
    private static final Function F_REG_PAIR = LIB_OBJC.getFunction("objc_registerClassPair");
    private static final Function F_ADD_METHOD = LIB_OBJC.getFunction("class_addMethod");
    private static final Function F_MSG_SEND = LIB_OBJC.getFunction("objc_msgSend");

    private static final Function F_CF_RUN = LIB_CF.getFunction("CFRunLoopRun");
    private static final Function F_CF_STOP = LIB_CF.getFunction("CFRunLoopStop");
    private static final Function F_CF_CURRENT = LIB_CF.getFunction("CFRunLoopGetCurrent");
    private static final AtomicBoolean RUN_LOOP_ACTIVE = new AtomicBoolean(true);
    private static final AtomicLong RUN_LOOP_REF = new AtomicLong(0L);

    private ObjC() {
    }

    public static long getClass(String name) {
        return asLong(F_GET_CLASS.invoke(Long.class, new Object[]{name}));
    }

    public static long sel(String name) {
        return asLong(F_SEL.invoke(Long.class, new Object[]{name}));
    }

    public static long allocateClassPair(long superclass, String name) {
        return asLong(F_ALLOC_PAIR.invoke(Long.class, new Object[]{superclass, name, 0L}));
    }

    public static void registerClassPair(long cls) {
        F_REG_PAIR.invoke(Void.class, new Object[]{cls});
    }

    public static boolean classAddMethod(long cls, long sel, Callback imp, String types) {
        Object result = F_ADD_METHOD.invoke(Integer.class, new Object[]{cls, sel, imp, types});
        return result != null && ((Integer) result) != 0;
    }

    public static long msgSend(long self, long sel, Object... args) {
        return asLong(F_MSG_SEND.invoke(Long.class, concat(self, sel, args)));
    }

    public static boolean msgSendBool(long self, long sel, Object... args) {
        Object result = F_MSG_SEND.invoke(Integer.class, concat(self, sel, args));
        return result != null && ((Integer) result) != 0;
    }

    public static long nsString(String value) {
        if (value == null) {
            return 0L;
        }
        long cls = getClass("NSString");
        long selector = sel("stringWithUTF8String:");
        return asLong(F_MSG_SEND.invoke(Long.class, new Object[]{cls, selector, value}));
    }

    public static String fromNSString(long nsStr) {
        if (nsStr == 0L) {
            return null;
        }
        long cString = msgSend(nsStr, sel("UTF8String"));
        if (cString == 0L) {
            return null;
        }
        return new Pointer(cString).getString(0, "UTF-8");
    }

    public static long nsData(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return 0L;
        }
        com.sun.jna.Memory memory = new com.sun.jna.Memory(bytes.length);
        memory.write(0, bytes, 0, bytes.length);
        long cls = getClass("NSData");
        long selector = sel("dataWithBytes:length:");
        return asLong(F_MSG_SEND.invoke(Long.class,
                new Object[]{cls, selector, memory, (long) bytes.length}));
    }

    public static long nsArrayOf(long obj) {
        long cls = getClass("NSArray");
        return msgSend(cls, sel("arrayWithObject:"), obj);
    }

    public static long allocInit(long cls) {
        long allocated = msgSend(cls, sel("alloc"));
        return msgSend(allocated, sel("init"));
    }

    public static void cfRunLoopRun() {
        F_CF_RUN.invoke(Void.class, new Object[]{});
    }

    public static void cfRunLoopRunUntilStopped() {
        RUN_LOOP_ACTIVE.set(true);
        RUN_LOOP_REF.set(asLong(F_CF_CURRENT.invoke(Long.class, new Object[]{})));
        while (RUN_LOOP_ACTIVE.get()) {
            cfRunLoopRun();
            if (RUN_LOOP_ACTIVE.get()) {
                try {
                    Thread.sleep(25L);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        RUN_LOOP_REF.set(0L);
    }

    public static void cfRunLoopStop() {
        RUN_LOOP_ACTIVE.set(false);
        long runLoop = RUN_LOOP_REF.get();
        if (runLoop != 0L) {
            F_CF_STOP.invoke(Void.class, new Object[]{runLoop});
        }
    }

    private static Object[] concat(long self, long sel, Object[] rest) {
        Object[] all = new Object[2 + rest.length];
        all[0] = self;
        all[1] = sel;
        System.arraycopy(rest, 0, all, 2, rest.length);
        return all;
    }

    static long asLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value) & 0xFFFFFFFFL;
        }
        if (value instanceof Pointer) {
            return Pointer.nativeValue((Pointer) value);
        }
        return 0L;
    }
}
