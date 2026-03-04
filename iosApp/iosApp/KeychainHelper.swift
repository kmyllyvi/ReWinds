import Foundation
import Security

class KeychainHelper {
    static let shared = KeychainHelper()

    private let service = "com.km.rewinds"
    private let account = "anthropic_api_key"

    /// Save API key to Keychain
    func save(_ key: String) -> Bool {
        let data = key.data(using: .utf8)!

        // Check if key already exists
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]

        // Delete existing key if it exists
        SecItemDelete(query as CFDictionary)

        // Add new key
        let attributes: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]

        let status = SecItemAdd(attributes as CFDictionary, nil)

        if status == errSecSuccess {
            print("✓ API key saved to Keychain")
            return true
        } else {
            print("✗ Failed to save API key to Keychain (status: \(status))")
            return false
        }
    }

    /// Load API key from Keychain
    func load() -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        if status == errSecSuccess, let data = result as? Data, let key = String(data: data, encoding: .utf8) {
            print("✓ API key loaded from Keychain")
            return key
        } else if status == errSecItemNotFound {
            print("ℹ No API key found in Keychain")
            return nil
        } else {
            print("✗ Failed to load API key from Keychain (status: \(status))")
            return nil
        }
    }

    /// Delete API key from Keychain
    func delete() -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]

        let status = SecItemDelete(query as CFDictionary)

        if status == errSecSuccess || status == errSecItemNotFound {
            print("✓ API key deleted from Keychain")
            return true
        } else {
            print("✗ Failed to delete API key from Keychain (status: \(status))")
            return false
        }
    }
}
