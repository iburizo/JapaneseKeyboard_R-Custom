import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # Look for onCreate or init block in IMEService
    target = """    override fun onCreate() {"""
    replacement = """    override fun onCreate() {
        KeyboardDefaultLayouts.uExtensionModeProvider = { AppPreference.sumire_u_extension_mode_preference }"""

    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, 'w') as f:
            f.write(content)
        print("Patched IMEService.kt")
    else:
        print("Could not find onCreate in IMEService.kt")

modify_file('app/src/main/java/com/kazumaproject/markdownhelperkeyboard/ime_service/IMEService.kt')
