import re

def fix(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    target1 = """            DisplayAction(
                KeyAction.VoiceInput,
                context.getString(R.string.voice_input),
                com.kazumaproject.core.R.drawable.settings_voice_24px
            )
        )"""
        
    replacement1 = """            DisplayAction(
                KeyAction.VoiceInput,
                context.getString(R.string.voice_input),
                com.kazumaproject.core.R.drawable.settings_voice_24px
            ),
            DisplayAction(
                KeyAction.Text("っ"),
                "促音 (っ)",
                com.kazumaproject.core.R.drawable.sokuon_icon
            )
        )"""

    if target1 in content:
        content = content.replace(target1, replacement1)

    target2 = """            KeyAction.VoiceInput -> com.kazumaproject.core.R.drawable.settings_voice_24px"""
    replacement2 = """            KeyAction.VoiceInput -> com.kazumaproject.core.R.drawable.settings_voice_24px
            KeyAction.Text("っ") -> com.kazumaproject.core.R.drawable.sokuon_icon"""
            
    if target2 in content:
        content = content.replace(target2, replacement2)

    with open(file_path, 'w') as f:
        f.write(content)

fix('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/data/KeyActionMapper.kt')
