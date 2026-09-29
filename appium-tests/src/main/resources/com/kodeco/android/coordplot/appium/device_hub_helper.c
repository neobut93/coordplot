// Runs inside a foldable iOS Simulator (via `xcrun simctl spawn`) and posts the same private
// vendor-defined HID events that Xcode's Device Hub sends for its orientation picker and hinge slider.
//
//   device_hub_helper orientation <portrait|landscape-left|landscape-right|pud>
//   device_hub_helper hinge <degrees>
//
// On the iPhone Duo the virtual machine provider republishes orientation and overwrites an ordinary
// rotation (XCUIDevice.orientation, which Appium/WebDriverAgent uses), so this is the route that sticks.
//
// Private and undocumented; verified by others on Xcode 27.1 only. Payload layout from:
//   https://github.com/artemnovichkov/hinge (skills/hinge/scripts/hinge_helper.c, MIT)
//   https://github.com/Mastersam07/OpenDeviceHub/pull/64 (FoldableControl.swift, MIT)
//   https://github.com/callstack/agent-device (apple/fold-helper/Fold.m, MIT)

#include <CoreFoundation/CoreFoundation.h>
#include <mach/mach_time.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

typedef struct __IOHIDEvent *IOHIDEventRef;
typedef struct __IOHIDEventSystemClient *IOHIDEventSystemClientRef;

extern IOHIDEventRef IOHIDEventCreateVendorDefinedEvent(CFAllocatorRef allocator, uint64_t timeStamp,
    uint32_t usagePage, uint32_t usage, uint32_t version, const uint8_t *data, CFIndex length, uint32_t options);
extern IOHIDEventSystemClientRef IOHIDEventSystemClientCreateWithType(CFAllocatorRef allocator, int type,
    CFDictionaryRef properties);
extern void IOHIDEventSystemClientDispatchEvent(IOHIDEventSystemClientRef client, IOHIDEventRef event);
extern CFDataRef IOCFSerialize(CFTypeRef object, CFOptionFlags options);

enum {
    kUsagePage = 0xFF61,
    kUsage = 0x5B,
    kClientTypeSimple = 4,
    kSerializeBinary = 1,
};

static int send_control(IOHIDEventSystemClientRef client, CFStringRef source, CFStringRef type, CFTypeRef value) {
    const void *keys[] = {CFSTR("provider"), CFSTR("source"), CFSTR("type"), CFSTR("value")};
    const void *values[] = {CFSTR("com.apple.Virtualization.VirtualMachines"), source, type, value};
    CFDictionaryRef payload = CFDictionaryCreate(NULL, keys, values, 4,
        &kCFTypeDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);
    CFDataRef data = IOCFSerialize(payload, kSerializeBinary);
    CFRelease(payload);
    if (!data) {
        fprintf(stderr, "device_hub_helper: could not serialize the report\n");
        return 1;
    }

    IOHIDEventRef event = IOHIDEventCreateVendorDefinedEvent(NULL, mach_absolute_time(), kUsagePage, kUsage, 0,
        CFDataGetBytePtr(data), CFDataGetLength(data), 0);
    CFRelease(data);
    if (!event) {
        fprintf(stderr, "device_hub_helper: could not create the HID event\n");
        return 1;
    }
    IOHIDEventSystemClientDispatchEvent(client, event);
    CFRelease(event);
    return 0;
}

static int is_orientation(const char *name) {
    return strcmp(name, "portrait") == 0 || strcmp(name, "landscape-left") == 0
        || strcmp(name, "landscape-right") == 0 || strcmp(name, "pud") == 0;
}

static int usage(void) {
    fprintf(stderr, "usage: device_hub_helper orientation <portrait|landscape-left|landscape-right|pud>\n"
                    "       device_hub_helper hinge <degrees>\n");
    return 64;
}

int main(int argc, char **argv) {
    if (argc != 3) return usage();

    IOHIDEventSystemClientRef client = IOHIDEventSystemClientCreateWithType(NULL, kClientTypeSimple, NULL);
    if (!client) {
        fprintf(stderr, "device_hub_helper: failed to create HID event system client\n");
        return 1;
    }

    int status;
    if (strcmp(argv[1], "orientation") == 0 && is_orientation(argv[2])) {
        CFStringRef value = CFStringCreateWithCString(NULL, argv[2], kCFStringEncodingUTF8);
        status = send_control(client, CFSTR("orientation-picker-control"), CFSTR("enum"), value);
        CFRelease(value);
    } else if (strcmp(argv[1], "hinge") == 0) {
        double degrees = atof(argv[2]);
        if (degrees < 0) degrees = 0;
        if (degrees > 180) degrees = 180;
        CFNumberRef value = CFNumberCreate(NULL, kCFNumberDoubleType, &degrees);
        status = send_control(client, CFSTR("hinge-slider-control"), CFSTR("range"), value);
        CFRelease(value);
    } else {
        return usage();
    }

    // Give the event system a moment to deliver before the process exits.
    usleep(100000);
    return status;
}
