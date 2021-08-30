# OneFileSystem (OFS)
Имплементация файловой системы, хранящейся в одном файле. 
Реализован собственный `FileSystemProvider`, так что можно использовать как обычную нестандартную файловую систему.
В модуле `Example` находится пример, который:
1. Создает файловую систему OFS на основе временного файла.
2. Копирует файл изображения из папки с ресурсами в OFS
3. Загружает файл изображения из OFS и отображает его.

В проекте FileSystem находится собственно имплементация. Особенности:
1. Файловая система выделяет память блоками по 1 кБ. 
1. Блоки освобождаются при удалении файлов и потом туда пишутся новые файлы.
1. Максимальный поддерживаемый размер файла около 200 килобайт.
1. Поддерживается только однопоточный доступ.
1. Метаинформация содержит заглушки времени создания/удаления/последней модификации.
1. В тестах можно найти примеры использования, особенно в `FileSystem/src/test/java/ofs/JavaPathsAndFilesAPIIntegrationTest.java`
1. Тесты требуют форка JVM для каждого класса. 

👍

