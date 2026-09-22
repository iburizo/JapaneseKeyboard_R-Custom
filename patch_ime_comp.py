import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # Search for override fun setComposingText
    target = """    override fun setComposingText(p0: CharSequence?, p1: Int): Boolean {
        val connection = currentInputConnection ?: return false"""
    
    replacement = """    override fun setComposingText(p0: CharSequence?, p1: Int): Boolean {
        val connection = currentInputConnection ?: return false
        val isComposing = !p0.isNullOrEmpty()
        mainLayoutBinding?.customLayoutDefault?.isComposing = isComposing
        splitInputs.values.forEach {
            it.binding.customLayoutFloating.isComposing = isComposing
        }"""
        
    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, 'w') as f:
            f.write(content)
        print("Patched IMEService.kt for isComposing sync")
    else:
        print("Could not find setComposingText in IMEService.kt")

modify_file('app/src/main/java/com/kazumaproject/markdownhelperkeyboard/ime_service/IMEService.kt')
