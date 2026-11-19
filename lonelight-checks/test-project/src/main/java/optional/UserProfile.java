// 检查：TypeSuffixCheck
// optional 包接受 DTO 和 Request 后缀，UserProfile 是反例。

package optional;

public record UserProfile(String name) {
    record AddressDTO(String city) { }

    record AddressRequest(String city) { }
}
