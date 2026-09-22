import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    target = "commitAndClearInput(mainView, converted)"
    replacement = "commitAndClearInput(converted)"
    content = content.replace(target, replacement)
    
    with open(file_path, 'w') as f:
        f.write(content)

modify_file('app/src/main/java/com/kazumaproject/markdownhelperkeyboard/ime_service/IMEService.kt')
