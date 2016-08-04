<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace gbksoft\multilingual;

use yii\base\Module as BaseModule;
use yii\i18n\PhpMessageSource;

/**
 * Class Module
 */
class Module extends BaseModule
{
    /**
     * @inheritdoc
     */
    public function init()
    {
        parent::init();
        \Yii::configure($this, require(__DIR__ . '/config/main.php'));

        $this->registerTranslations();
    }

    /**
     * RegisterTranslations
     */
    private function registerTranslations()
    {
        \Yii::$app->i18n->translations['language-backend'] = [
            'class' => PhpMessageSource::class,
            'sourceLanguage' => 'en',
            'basePath' => '@language/messages'
        ];
    }
}
