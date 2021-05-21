import os
#os.system('pipenv shell')
#os.system('pipenv install pytesseract Pillow')
try:
    from PIL import Image
except ImportError:
    import Image
import pytesseract

def ocr_core():
    filename = "../picture.jpg"
    text = pytesseract.image_to_string(Image.open(filename))  # We'll use Pillow's Image class to open the image and pytesseract to detect the string in the image
    file = open("ingredients.txt","w")
    file.write(text)
    file.close()
    if os.path.exists("./ingredients-bad.txt"):
        os.remove("./ingredients-bad.txt")
    os.chdir('../nlp')
    os.system('javac Finder.java')
    os.system('java Finder')
    os.chdir('../ocr_server') 
    file = open("ingredients-bad.txt", "r")
    lines = file.readlines()
    if (len(lines) == 0):
        return "This product looks safe! We didn't detect any harmful chemicals."
    ingredients = "This product contains harmful chemicals! "
    i = 0
    for line in lines:
        ingredients += str(i) + " - " + line
        i += 1
    print(ingredients)

ocr_core()
