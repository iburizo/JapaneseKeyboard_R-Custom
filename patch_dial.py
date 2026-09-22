import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    replacement = """
    private fun evaluateExpression(expr: String): String? {
        if (!expr.any { it in listOf('+', '-', '*', '/', '^', '%') }) return null
        return try {
            val result = CalculationParser().calculate(expr, AngleMode.DEGREES)
            if (result != null) {
                val decimalFormat = java.text.DecimalFormat("#.###")
                decimalFormat.format(result)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
"""
    # Replace the existing evaluateExpression
    content = re.sub(r'private fun evaluateExpression\(.*?\).*?return null \n    \}', replacement.strip(), content, flags=re.DOTALL)
    
    with open(file_path, 'w') as f:
        f.write(content)

modify_file('app/src/main/java/com/kazumaproject/markdownhelperkeyboard/converter/utility/DialTimeDateConverter.kt')
