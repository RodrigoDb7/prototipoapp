Visão Geral
Este aplicativo Android, desenvolvido em Kotlin, implementa a metodologia SayMore para anotação linguística de conteúdo de áudio e vídeo. Projetado especificamente para linguistas de campo e especialistas em documentação de línguas, ele fornece um ambiente móvel para criar anotações alinhadas ao tempo com transcrições, traduções e atribuição de falantes.
Funcionalidades

Suporte a Áudio e Vídeo: Carregamento e reprodução de arquivos de áudio e vídeo
Visualização de Forma de Onda: Representação visual do áudio para anotação precisa
Marcadores Temporais: Crie marcações de tempo com transcrições e traduções associadas
Gerenciamento de Falantes: Crie e atribua falantes a segmentos específicos
Capacidades de Exportação: Exporte anotações em múltiplos formatos:

CSV (para análise em planilhas)
XML (para processamento personalizado)
ELAN (.eaf) para integração com o software linguístico ELAN



Detalhes de Implementação
O aplicativo segue uma arquitetura híbrida MVC/MVP com modelos de dados, utilitários de gerenciamento e componentes de UI claramente separados. Classes principais incluem:

Project: Contêiner para dados de anotação
AudioFile/VideoFile: Representações de arquivos de mídia
Marker: Anotação alinhada ao tempo com metadados
Speaker: Informações do participante
ExportManager: Gerencia exportação de dados em múltiplos formatos

Propósito
Esta ferramenta estabelece uma ponte entre a coleta de dados em campo e a análise linguística detalhada, fornecendo uma solução móvel para documentação inicial e anotação estruturada, compatível com ferramentas de análise desktop como o ELAN.
Requisitos

Android 7.0 (nível de API 24) ou superior
Permissões de armazenamento para acesso à mídia

Desenvolvimento
Este aplicativo foi desenvolvido como parte de um projeto de TCC explorando abordagens móveis para documentação linguística seguindo a metodologia SayMore.
