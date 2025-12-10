const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// Ruta al archivo serviceAccountKey.json
const serviceAccountPath = path.join(__dirname, '..', 'serviceAccountKey.json');

// Verificar que el archivo existe
if (!fs.existsSync(serviceAccountPath)) {
    console.error('❌ ERROR: No se encontró serviceAccountKey.json');
    console.error('   Asegúrate de que el archivo esté en la raíz del proyecto');
    console.error('   Ruta esperada:', serviceAccountPath);
    process.exit(1);
}

// Cargar las credenciales
let serviceAccount;
try {
    serviceAccount = require(serviceAccountPath);
} catch (error) {
    console.error('❌ ERROR: No se pudo cargar serviceAccountKey.json');
    console.error('   Verifica que el archivo sea un JSON válido');
    console.error('   Error:', error.message);
    process.exit(1);
}

// Inicializar Firebase Admin
try {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
    console.log('✅ Firebase Admin inicializado correctamente');
} catch (error) {
    console.error('❌ ERROR: No se pudo inicializar Firebase Admin');
    console.error('   Error:', error.message);
    process.exit(1);
}

const db = admin.firestore();

// Función para inicializar Firestore con datos base
async function initFirestore() {
    console.log('🚀 Iniciando inicialización de Firestore...\n');

    try {
        console.log('📊 Proyecto:', serviceAccount.project_id);
        console.log('📧 Cliente Email:', serviceAccount.client_email);
        console.log('');

        // Crear documento de sistema para verificar la conexión
        const testDocRef = db.collection('_system').doc('init');
        await testDocRef.set({
            initialized: true,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            version: '1.0.0'
        });
        console.log('✅ Documento de sistema creado en _system/init');

        // ============================================
        // CREAR DATOS DE PRUEBA PARA EDUQUIZ
        // ============================================

        const now = Date.now();
        const packId = 'pack_2025_w01';
        const weekLabel = '2025-W01';

        console.log('\n📦 Creando Pack de prueba...');
        
        // 1. Crear Pack
        const packRef = db.collection('packs').doc(packId);
        await packRef.set({
            packId: packId,
            weekLabel: weekLabel,
            status: 'PUBLISHED',
            publishedAt: now,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        console.log(`   ✅ Pack creado: ${packId}`);

        // 2. Crear Textos de Lectura
        console.log('\n📚 Creando textos de lectura...');
        
        const texts = [
            {
                textId: 'txt_2025_w01_001',
                packId: packId,
                title: 'La Energía Solar en las Ciudades',
                body: `La energía solar se ha convertido en una de las fuentes de energía renovable más prometedoras para las ciudades modernas. Los paneles solares instalados en los techos de edificios pueden generar electricidad suficiente para abastecer a miles de hogares. Además, esta tecnología ayuda a reducir las emisiones de carbono y la dependencia de combustibles fósiles.

Las ciudades que invierten en energía solar no solo contribuyen al cuidado del medio ambiente, sino que también reducen sus costos energéticos a largo plazo. Los expertos predicen que en los próximos años, la mayoría de las ciudades incorporarán sistemas de energía solar como parte de su infraestructura básica.`,
                subject: 'LECTURA'
            },
            {
                textId: 'txt_2025_w01_002',
                packId: packId,
                title: 'Problema de Geometría: Área de un Triángulo',
                body: `Un triángulo tiene una base de 12 centímetros y una altura de 8 centímetros. Para calcular su área, debemos usar la fórmula: Área = (base × altura) / 2.

En este caso: Área = (12 × 8) / 2 = 96 / 2 = 48 centímetros cuadrados.

Si duplicamos la base y mantenemos la misma altura, el área se duplicaría. Si duplicamos tanto la base como la altura, el área se cuadruplicaría.`,
                subject: 'MATEMATICA'
            },
            {
                textId: 'txt_2025_w01_003',
                packId: packId,
                title: 'El Ciclo del Agua',
                body: `El ciclo del agua es un proceso fundamental para la vida en la Tierra. Comienza cuando el sol calienta el agua de los océanos, lagos y ríos, causando que se evapore y se convierta en vapor de agua. Este vapor asciende a la atmósfera donde se enfría y se condensa formando nubes.

Cuando las nubes se saturan, el agua cae de vuelta a la Tierra en forma de precipitación (lluvia, nieve o granizo). El agua que cae puede infiltrarse en el suelo, fluir hacia ríos y océanos, o ser absorbida por las plantas. Este ciclo se repite continuamente, manteniendo el equilibrio del agua en nuestro planeta.`,
                subject: 'CIENCIAS'
            }
        ];

        for (const text of texts) {
            const textRef = db.collection('texts').doc(text.textId);
            await textRef.set({
                ...text,
                gradeBand: 'PISA',
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                status: 'PUBLISHED'
            });
            console.log(`   ✅ Texto creado: ${text.textId} (${text.subject})`);
        }

        // 3. Crear Preguntas y Opciones
        console.log('\n❓ Creando preguntas y opciones...');

        const questions = [
            // Pregunta 1 - LECTURA
            {
                questionId: 'q_2025_w01_0001',
                textId: 'txt_2025_w01_001',
                packId: packId,
                prompt: '¿Cuál es la idea principal del texto sobre la energía solar?',
                correctOptionId: 'B',
                difficulty: 2,
                explanationText: 'La idea principal es que la energía solar es una fuente renovable prometedora que ayuda a las ciudades a reducir emisiones y costos.',
                explanationStatus: 'APPROVED',
                options: [
                    { optionId: 'A', text: 'Los paneles solares son muy caros de instalar' },
                    { optionId: 'B', text: 'La energía solar es una fuente renovable prometedora para las ciudades' },
                    { optionId: 'C', text: 'Solo algunas ciudades pueden usar energía solar' },
                    { optionId: 'D', text: 'La energía solar no es confiable' }
                ]
            },
            // Pregunta 2 - LECTURA
            {
                questionId: 'q_2025_w01_0002',
                textId: 'txt_2025_w01_001',
                packId: packId,
                prompt: 'Según el texto, ¿qué beneficio adicional obtienen las ciudades que invierten en energía solar?',
                correctOptionId: 'C',
                difficulty: 1,
                explanationText: 'El texto menciona explícitamente que las ciudades reducen sus costos energéticos a largo plazo.',
                explanationStatus: 'APPROVED',
                options: [
                    { optionId: 'A', text: 'Aumentan su población' },
                    { optionId: 'B', text: 'Mejoran su transporte público' },
                    { optionId: 'C', text: 'Reducen sus costos energéticos a largo plazo' },
                    { optionId: 'D', text: 'Construyen más edificios' }
                ]
            },
            // Pregunta 3 - MATEMATICA
            {
                questionId: 'q_2025_w01_0003',
                textId: 'txt_2025_w01_002',
                packId: packId,
                prompt: 'Si un triángulo tiene base de 12 cm y altura de 8 cm, ¿cuál es su área?',
                correctOptionId: 'D',
                difficulty: 1,
                explanationText: 'Área = (base × altura) / 2 = (12 × 8) / 2 = 96 / 2 = 48 cm²',
                explanationStatus: 'APPROVED',
                options: [
                    { optionId: 'A', text: '20 cm²' },
                    { optionId: 'B', text: '32 cm²' },
                    { optionId: 'C', text: '40 cm²' },
                    { optionId: 'D', text: '48 cm²' }
                ]
            },
            // Pregunta 4 - MATEMATICA
            {
                questionId: 'q_2025_w01_0004',
                textId: 'txt_2025_w01_002',
                packId: packId,
                prompt: 'Si duplicamos tanto la base como la altura de un triángulo, ¿qué sucede con su área?',
                correctOptionId: 'B',
                difficulty: 2,
                explanationText: 'Si duplicamos base y altura: Área nueva = (2b × 2h) / 2 = 4(bh/2) = 4 × área original. El área se cuadruplica.',
                explanationStatus: 'APPROVED',
                options: [
                    { optionId: 'A', text: 'Se duplica' },
                    { optionId: 'B', text: 'Se cuadruplica' },
                    { optionId: 'C', text: 'Se mantiene igual' },
                    { optionId: 'D', text: 'Se reduce a la mitad' }
                ]
            },
            // Pregunta 5 - CIENCIAS
            {
                questionId: 'q_2025_w01_0005',
                textId: 'txt_2025_w01_003',
                packId: packId,
                prompt: '¿Qué proceso ocurre cuando el vapor de agua se enfría en la atmósfera?',
                correctOptionId: 'A',
                difficulty: 1,
                explanationText: 'La condensación es el proceso por el cual el vapor de agua se enfría y se convierte en gotas de agua, formando nubes.',
                explanationStatus: 'APPROVED',
                options: [
                    { optionId: 'A', text: 'Condensación' },
                    { optionId: 'B', text: 'Evaporación' },
                    { optionId: 'C', text: 'Precipitación' },
                    { optionId: 'D', text: 'Infiltración' }
                ]
            },
            // Pregunta 6 - CIENCIAS
            {
                questionId: 'q_2025_w01_0006',
                textId: 'txt_2025_w01_003',
                packId: packId,
                prompt: '¿Qué fuerza principal impulsa el ciclo del agua?',
                correctOptionId: 'C',
                difficulty: 2,
                explanationText: 'El sol proporciona la energía necesaria para calentar el agua y causar la evaporación, que es el primer paso del ciclo del agua.',
                explanationStatus: 'APPROVED',
                options: [
                    { optionId: 'A', text: 'El viento' },
                    { optionId: 'B', text: 'La gravedad' },
                    { optionId: 'C', text: 'La energía del sol' },
                    { optionId: 'D', text: 'La presión atmosférica' }
                ]
            }
        ];

        for (const question of questions) {
            // Crear la pregunta
            const questionRef = db.collection('questions').doc(question.questionId);
            await questionRef.set({
                questionId: question.questionId,
                textId: question.textId,
                packId: question.packId,
                prompt: question.prompt,
                correctOptionId: question.correctOptionId,
                difficulty: question.difficulty,
                explanationText: question.explanationText,
                explanationStatus: question.explanationStatus,
                options: question.options,
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                status: 'PUBLISHED',
                tags: question.textId.includes('LECTURA') ? ['comprension', 'inferencia'] :
                      question.textId.includes('MATEMATICA') ? ['geometria', 'calculo'] :
                      ['ciencias', 'naturaleza']
            });
            console.log(`   ✅ Pregunta creada: ${question.questionId} (${question.correctOptionId} es correcta)`);
        }

        // 4. Actualizar Pack con referencias
        await packRef.update({
            textIds: texts.map(t => t.textId),
            questionIds: questions.map(q => q.questionId),
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        console.log('\n✅ Pack actualizado con referencias a textos y preguntas');

        // Resumen
        console.log('\n📊 Resumen de datos creados:');
        console.log(`   📦 Packs: 1`);
        console.log(`   📚 Textos: ${texts.length}`);
        console.log(`   ❓ Preguntas: ${questions.length}`);
        console.log(`   📝 Opciones totales: ${questions.reduce((sum, q) => sum + q.options.length, 0)}`);
        
        console.log('\n✅ Firestore inicializado correctamente con datos de prueba');

    } catch (error) {
        console.error('❌ Error al inicializar Firestore:', error);
        console.error('   Detalles:', error.message);
        process.exit(1);
    }
}

// Ejecutar la inicialización
initFirestore()
    .then(() => {
        console.log('\n✨ Inicialización completada exitosamente');
        console.log('💡 Puedes verificar los datos en Firebase Console');
        process.exit(0);
    })
    .catch((error) => {
        console.error('\n❌ Error fatal durante la inicialización:', error);
        process.exit(1);
    });

