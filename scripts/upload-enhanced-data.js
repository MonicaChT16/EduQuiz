const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// ============================================
// CONFIGURACIÓN
// ============================================

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

// ============================================
// DATOS PERSONALIZADOS
// ============================================
// MODIFICA ESTA SECCIÓN CON TUS PROPIOS DATOS

async function uploadEnhancedData() {
    console.log('🚀 Subiendo datos mejorados a Firestore...\n');

    try {
        console.log('📊 Proyecto:', serviceAccount.project_id);
        console.log('📧 Cliente Email:', serviceAccount.client_email);
        console.log('');

        const now = Date.now();

        // ============================================
        // 1. CREAR PACKS
        // ============================================
        console.log('📦 Creando packs...');

        const packs = [
            {
                packId: 'pack_2025_w02',
                weekLabel: '2025-W02',
                status: 'PUBLISHED',
                publishedAt: now
            },
            {
                packId: 'pack_2025_w03',
                weekLabel: '2025-W03',
                status: 'PUBLISHED',
                publishedAt: now + (7 * 24 * 60 * 60 * 1000) // 7 días después
            }
            // Agrega más packs aquí
        ];

        for (const pack of packs) {
            const packRef = db.collection('packs').doc(pack.packId);
            await packRef.set({
                packId: pack.packId,
                weekLabel: pack.weekLabel,
                status: pack.status,
                publishedAt: pack.publishedAt,
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            console.log(`   ✅ Pack creado: ${pack.packId}`);
        }

        // ============================================
        // 2. CREAR TEXTOS
        // ============================================
        console.log('\n📚 Creando textos de lectura...');

        const texts = [
            // Pack 2025-W02
            {
                textId: 'txt_2025_w02_001',
                packId: 'pack_2025_w02',
                title: 'El Cambio Climático y sus Efectos',
                body: `El cambio climático es uno de los desafíos más importantes que enfrenta la humanidad en el siglo XXI. Las emisiones de gases de efecto invernadero, principalmente dióxido de carbono, están causando un aumento gradual de la temperatura global. Este fenómeno tiene consecuencias directas en los ecosistemas, la agricultura, y la vida humana.

Los científicos han documentado cambios significativos en los patrones climáticos, incluyendo el derretimiento de los glaciares, el aumento del nivel del mar, y eventos climáticos extremos más frecuentes. La comunidad internacional ha reconocido la urgencia de tomar medidas para reducir las emisiones y adaptarse a los cambios que ya están ocurriendo.

Las soluciones al cambio climático requieren una combinación de políticas gubernamentales, innovación tecnológica, y cambios en el comportamiento individual. La transición a energías renovables, la mejora de la eficiencia energética, y la protección de los bosques son estrategias clave para mitigar los efectos del cambio climático.`,
                subject: 'COMPRENSION_LECTORA'
            },
            {
                textId: 'txt_2025_w02_002',
                packId: 'pack_2025_w02',
                title: 'Problema de Álgebra: Ecuaciones Lineales',
                body: `Una ecuación lineal es una expresión matemática que relaciona dos cantidades mediante una igualdad. Por ejemplo, la ecuación 2x + 3 = 11 representa una relación donde debemos encontrar el valor de x que hace verdadera la igualdad.

Para resolver esta ecuación, seguimos estos pasos:
1. Restamos 3 de ambos lados: 2x = 11 - 3 = 8
2. Dividimos ambos lados por 2: x = 8 / 2 = 4

Por lo tanto, x = 4 es la solución de la ecuación.

Las ecuaciones lineales son fundamentales en matemáticas porque nos permiten modelar situaciones del mundo real, como calcular costos, determinar velocidades, o resolver problemas de proporciones.`,
                subject: 'MATEMATICA'
            },
            {
                textId: 'txt_2025_w02_003',
                packId: 'pack_2025_w02',
                title: 'La Fotosíntesis: Proceso Vital',
                body: `La fotosíntesis es el proceso mediante el cual las plantas, algas y algunas bacterias convierten la energía de la luz solar en energía química. Este proceso es esencial para la vida en la Tierra, ya que produce el oxígeno que respiramos y los compuestos orgánicos que sirven como alimento.

El proceso de fotosíntesis ocurre principalmente en las hojas de las plantas, dentro de estructuras llamadas cloroplastos. Estos contienen clorofila, un pigmento verde que captura la energía de la luz solar.

La ecuación general de la fotosíntesis es:
6CO₂ + 6H₂O + energía luminosa → C₆H₁₂O₆ + 6O₂

Esto significa que las plantas toman dióxido de carbono del aire y agua del suelo, y con la energía del sol, producen glucosa (un tipo de azúcar) y oxígeno. La glucosa se usa como fuente de energía para la planta, mientras que el oxígeno se libera a la atmósfera.`,
                subject: 'CIENCIAS'
            }
            // Agrega más textos aquí para otros packs
        ];

        for (const text of texts) {
            const textRef = db.collection('texts').doc(text.textId);
            await textRef.set({
                textId: text.textId,
                packId: text.packId,
                title: text.title,
                body: text.body,
                subject: text.subject,
                gradeBand: 'PISA',
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                status: 'PUBLISHED'
            });
            console.log(`   ✅ Texto creado: ${text.textId} (${text.subject})`);
        }

        // ============================================
        // 3. CREAR PREGUNTAS Y OPCIONES
        // ============================================
        console.log('\n❓ Creando preguntas y opciones...');

        const questions = [
            // Preguntas para txt_2025_w02_001 (COMPRENSION_LECTORA)
            {
                questionId: 'q_2025_w02_0001',
                textId: 'txt_2025_w02_001',
                packId: 'pack_2025_w02',
                prompt: '¿Cuál es la causa principal del cambio climático según el texto?',
                correctOptionId: 'B',
                difficulty: 1,
                explanationText: 'El texto menciona explícitamente que las emisiones de gases de efecto invernadero, principalmente dióxido de carbono, están causando el aumento de la temperatura global.',
                explanationStatus: 'APPROVED',
                options: [
                    { optionId: 'A', text: 'El aumento de la población mundial' },
                    { optionId: 'B', text: 'Las emisiones de gases de efecto invernadero' },
                    { optionId: 'C', text: 'La deforestación exclusivamente' },
                    { optionId: 'D', text: 'Los cambios naturales del clima' }
                ]
            },
            {
                questionId: 'q_2025_w02_0002',
                textId: 'txt_2025_w02_001',
                packId: 'pack_2025_w02',
                prompt: 'Según el texto, ¿qué estrategia NO se menciona como solución al cambio climático?',
                correctOptionId: 'D',
                difficulty: 2,
                explanationText: 'El texto menciona la transición a energías renovables, la mejora de la eficiencia energética, y la protección de los bosques, pero no menciona la construcción de más represas.',
                explanationStatus: 'APPROVED',
                options: [
                    { optionId: 'A', text: 'Transición a energías renovables' },
                    { optionId: 'B', text: 'Mejora de la eficiencia energética' },
                    { optionId: 'C', text: 'Protección de los bosques' },
                    { optionId: 'D', text: 'Construcción de más represas' }
                ]
            },
            // Preguntas para txt_2025_w02_002 (MATEMATICA)
            {
                questionId: 'q_2025_w02_0003',
                textId: 'txt_2025_w02_002',
                packId: 'pack_2025_w02',
                prompt: 'Si tenemos la ecuación 3x - 5 = 10, ¿cuál es el valor de x?',
                correctOptionId: 'C',
                difficulty: 1,
                explanationText: 'Resolvemos: 3x - 5 = 10 → 3x = 10 + 5 → 3x = 15 → x = 15 / 3 = 5',
                explanationStatus: 'APPROVED',
                options: [
                    { optionId: 'A', text: '3' },
                    { optionId: 'B', text: '4' },
                    { optionId: 'C', text: '5' },
                    { optionId: 'D', text: '6' }
                ]
            },
            {
                questionId: 'q_2025_w02_0004',
                textId: 'txt_2025_w02_002',
                packId: 'pack_2025_w02',
                prompt: 'Si una ecuación lineal tiene la forma ax + b = c, y queremos despejar x, ¿qué operación debemos hacer primero?',
                correctOptionId: 'A',
                difficulty: 2,
                explanationText: 'Para despejar x, primero debemos aislar el término con x. Si tenemos ax + b = c, primero restamos b de ambos lados para obtener ax = c - b, y luego dividimos por a.',
                explanationStatus: 'APPROVED',
                options: [
                    { optionId: 'A', text: 'Restar b de ambos lados' },
                    { optionId: 'B', text: 'Dividir ambos lados por a' },
                    { optionId: 'C', text: 'Multiplicar ambos lados por a' },
                    { optionId: 'D', text: 'Sumar b a ambos lados' }
                ]
            },
            // Preguntas para txt_2025_w02_003 (CIENCIAS)
            {
                questionId: 'q_2025_w02_0005',
                textId: 'txt_2025_w02_003',
                packId: 'pack_2025_w02',
                prompt: '¿Dónde ocurre principalmente el proceso de fotosíntesis en las plantas?',
                correctOptionId: 'B',
                difficulty: 1,
                explanationText: 'El texto indica que la fotosíntesis ocurre principalmente en las hojas de las plantas, dentro de estructuras llamadas cloroplastos.',
                explanationStatus: 'APPROVED',
                options: [
                    { optionId: 'A', text: 'En las raíces' },
                    { optionId: 'B', text: 'En las hojas' },
                    { optionId: 'C', text: 'En el tallo' },
                    { optionId: 'D', text: 'En las flores' }
                ]
            },
            {
                questionId: 'q_2025_w02_0006',
                textId: 'txt_2025_w02_003',
                packId: 'pack_2025_w02',
                prompt: 'Según la ecuación de fotosíntesis, ¿qué productos se generan?',
                correctOptionId: 'C',
                difficulty: 2,
                explanationText: 'La ecuación muestra que los productos de la fotosíntesis son glucosa (C₆H₁₂O₆) y oxígeno (6O₂).',
                explanationStatus: 'APPROVED',
                options: [
                    { optionId: 'A', text: 'Solo dióxido de carbono' },
                    { optionId: 'B', text: 'Solo agua' },
                    { optionId: 'C', text: 'Glucosa y oxígeno' },
                    { optionId: 'D', text: 'Solo glucosa' }
                ]
            }
            // Agrega más preguntas aquí
        ];

        for (const question of questions) {
            // Validar que la opción correcta exista
            const correctOptionExists = question.options.some(
                opt => opt.optionId === question.correctOptionId
            );
            
            if (!correctOptionExists) {
                console.error(`   ❌ ERROR: La pregunta ${question.questionId} tiene correctOptionId="${question.correctOptionId}" pero esa opción no existe`);
                continue;
            }

            // Validar que haya exactamente 4 opciones
            if (question.options.length !== 4) {
                console.error(`   ❌ ERROR: La pregunta ${question.questionId} debe tener exactamente 4 opciones, tiene ${question.options.length}`);
                continue;
            }

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
                options: question.options, // ⚠️ IMPORTANTE: Array de objetos, no subcolección
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                status: 'PUBLISHED',
                tags: question.textId.includes('LECTURA') || question.textId.includes('COMPRENSION') 
                    ? ['comprension', 'inferencia'] 
                    : question.textId.includes('MATEMATICA') 
                    ? ['algebra', 'calculo'] 
                    : ['ciencias', 'biologia']
            });
            console.log(`   ✅ Pregunta creada: ${question.questionId} (${question.correctOptionId} es correcta)`);
        }

        // ============================================
        // 4. ACTUALIZAR PACKS CON REFERENCIAS
        // ============================================
        console.log('\n🔗 Actualizando packs con referencias...');

        for (const pack of packs) {
            const packTexts = texts.filter(t => t.packId === pack.packId);
            const packQuestions = questions.filter(q => q.packId === pack.packId);
            
            const packRef = db.collection('packs').doc(pack.packId);
            await packRef.update({
                textIds: packTexts.map(t => t.textId),
                questionIds: packQuestions.map(q => q.questionId),
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            console.log(`   ✅ Pack ${pack.packId} actualizado:`);
            console.log(`      - Textos: ${packTexts.length}`);
            console.log(`      - Preguntas: ${packQuestions.length}`);
        }

        // ============================================
        // RESUMEN
        // ============================================
        console.log('\n📊 Resumen de datos creados:');
        console.log(`   📦 Packs: ${packs.length}`);
        console.log(`   📚 Textos: ${texts.length}`);
        console.log(`   ❓ Preguntas: ${questions.length}`);
        console.log(`   📝 Opciones totales: ${questions.reduce((sum, q) => sum + q.options.length, 0)}`);
        
        // Estadísticas por materia
        const textsBySubject = {};
        texts.forEach(t => {
            textsBySubject[t.subject] = (textsBySubject[t.subject] || 0) + 1;
        });
        console.log('\n📚 Textos por materia:');
        Object.entries(textsBySubject).forEach(([subject, count]) => {
            console.log(`   - ${subject}: ${count}`);
        });

        // Estadísticas por dificultad
        const questionsByDifficulty = {};
        questions.forEach(q => {
            questionsByDifficulty[q.difficulty] = (questionsByDifficulty[q.difficulty] || 0) + 1;
        });
        console.log('\n❓ Preguntas por dificultad:');
        Object.entries(questionsByDifficulty).forEach(([difficulty, count]) => {
            const level = difficulty === '1' ? 'Fácil' : difficulty === '2' ? 'Medio' : 'Difícil';
            console.log(`   - ${level} (${difficulty}): ${count}`);
        });

        console.log('\n✅ Datos mejorados subidos correctamente a Firestore');
        console.log('\n💡 Próximos pasos:');
        console.log('   1. Verifica los datos en Firebase Console');
        console.log('   2. Ejecuta: node scripts/verify-firestore.js');
        console.log('   3. Prueba descargar los packs en la app');

    } catch (error) {
        console.error('\n❌ Error al subir datos:', error);
        console.error('   Detalles:', error.message);
        if (error.stack) {
            console.error('   Stack:', error.stack);
        }
        process.exit(1);
    }
}

// Ejecutar la función
uploadEnhancedData()
    .then(() => {
        console.log('\n✨ Proceso completado exitosamente');
        process.exit(0);
    })
    .catch((error) => {
        console.error('\n❌ Error fatal durante la ejecución:', error);
        process.exit(1);
    });
