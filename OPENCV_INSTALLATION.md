# OpenCV Installation for Hospismart (Windows 10/11)

## 🔧 Installation Automatique (Recommandé)

### Option 1: Maven + Windows Native
OpenCV sera automatiquement téléchargé lors du premier build. Si OpenCV ne se charge pas, suivez ces étapes.

## 🔴 Si Erreur: "System.loadLibrary(Core.NATIVE_LIBRARY_NAME) failed"

### Option 2: Download des Binaries Manuels

1. **Télécharger OpenCV 4.8.1**
   - Allez sur https://opencv.org/releases/
   - Téléchargez **opencv-4.8.1-vc14_vc15.exe** (Windows)
   - Installez dans `C:\opencv-4.8.1\`

2. **Localiser la DLL**
   - Naviguez vers `C:\opencv-4.8.1\opencv\build\x64\vc15\bin\`
   - Trouvez `opencv_java481.dll`

3. **Ajouter au Classpath** - Deux méthodes

#### Méthode A (Rapide): Variable d'Environnement
```
Windows Key -> Edit environment variables
New User Variable:
    Name: OPENCV_DLL_PATH
    Value: C:\opencv-4.8.1\opencv\build\x64\vc15\bin
```

Puis relancez IntelliJ.

#### Méthode B (Fichier pom.xml - Recommandé)
Modifiez le `pom.xml` pour charger les natives automatiquement :

```xml
<properties>
    <opencv.version>4.8.1</opencv.version>
</properties>

<dependencies>
    <!-- ... autres dépendances ... -->
    <dependency>
        <groupId>org.opencv</groupId>
        <artifactId>opencv-java</artifactId>
        <version>${opencv.version}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Existing plugins... -->
        
        <!-- Charger les natives OpenCV -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-dependency-plugin</artifactId>
            <version>3.3.0</version>
            <executions>
                <execution>
                    <id>unpack-opencv-natives</id>
                    <phase>generate-resources</phase>
                    <goals>
                        <goal>unpack</goal>
                    </goals>
                    <configuration>
                        <artifactItems>
                            <artifactItem>
                                <groupId>org.opencv</groupId>
                                <artifactId>opencv-java</artifactId>
                                <version>${opencv.version}</version>
                                <classifier>windows-x86_64</classifier>
                                <type>jar</type>
                                <overWrite>false</overWrite>
                                <outputDirectory>
                                    ${project.build.directory}/opencv-natives
                                </outputDirectory>
                            </artifactItem>
                        </artifactItems>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <!-- Copier les DLLs au classpath -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-resources-plugin</artifactId>
            <version>3.3.0</version>
            <executions>
                <execution>
                    <id>copy-dlls</id>
                    <phase>prepare-package</phase>
                    <goals>
                        <goal>copy-resources</goal>
                    </goals>
                    <configuration>
                        <outputDirectory>
                            ${project.build.directory}/classes
                        </outputDirectory>
                        <resources>
                            <resource>
                                <directory>
                                    ${project.build.directory}/opencv-natives
                                </directory>
                                <includes>
                                    <include>*.dll</include>
                                    <include>*.so</include>
                                    <include>*.dylib</include>
                                </includes>
                            </resource>
                        </resources>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## ✅ Vérifier L'Installation

```bash
# Naviguez vers le dossier du projet
cd C:\Users\ashe8\OneDrive\Desktop\hospismart-desktop

# Téléchargez les dépendances
mvn dependency:resolve

# Compilez
mvn clean compile

# Exécutez
mvn javafx:run
```

## 🎯 Résolution des Problèmes Courants

### Erreur: "opencv_java481 not found in java.library.path"

**Cause**: Les DLLs natives OpenCV ne sont pas dans le chemin

**Solutions**:
1. Vérifiez que vous êtes en 64-bit JVM
   ```bash
   java -version  # Vérifiez "64-Bit Server VM"
   ```

2. Téléchargez manuellement depuis:
   https://github.com/opencv/opencv/releases/tag/4.8.1
   
3. Extrayez `opencv-java-4.8.1.dll` à:
   - `C:\Windows\System32\` (Administrateur requis!), OU
   - Dans `target/classes/` de votre projet, OU
   - Dans le même répertoire que votre JAR

### Erreur: "UnsatisfiedLinkError"

```bash
# Vérifiez votre version de Java
java -version

# Vous avez besoin de Java 11+
# Si < 11, mettez à jour
```

### Caméra ne marche pas

```bash
# Testez que la caméra est accessible
# Windows: Paramètres -> Confidentialité -> Caméra -> Autoriser

# Vérifiez les permissions dans les paramètres
# Assurez-vous que le port USB n'est pas utilisé par autre app
```

## 📦 Alternative: Maven Central

Si Maven n'arrive pas à télécharger OpenCV, ajoutez le repository:

```xml
<repositories>
    <repository>
        <id>opencv.repository.releases</id>
        <url>https://mvnrepository.com/artifact/org.opencv/opencv-java</url>
    </repository>
</repositories>
```

## 🔍 Debug OpenCV

Pour voir si OpenCV se charge correctement, lancez ce code simple:

```java
import org.opencv.core.Core;

public class TestOpenCV {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("OpenCV Version: " + Core.VERSION);
        System.out.println("✅ OpenCV Loaded Successfully!");
    }
}
```

## 📚 Resources Officielles

- [OpenCV Java Release](https://github.com/opencv/opencv/releases)
- [OpenCV Java Tutorial](https://docs.opencv.org/master/d9/df8/tutorial_root.html)
- [Setup OpenCV Windows](https://docs.opencv.org/master/d3/d52/tutorial_windows_setup.html)

---

## ✅ Verification Finale

Une fois tout configuré, testez:

```bash
mvn clean javafx:run
```

Si vous voyez:
```
[FaceRecognition] Caméra initialisée avec succès
✅ Connexion au projet Hospismart : OK
```

✅ **OpenCV est correctement installé !**

---

**Questions?** Consultez `FACIAL_RECOGNITION_GUIDE.md` pour plus d'aide.
