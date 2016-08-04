<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */

use yii\helpers\Html;

/* @var $this yii\web\View */
/* @var gbksoft\multilingual\models\Language $model */

$this->title = Yii::t('language-backend', 'Create Language');
$this->params['breadcrumbs'][] = ['label' => Yii::t('language-backend', 'Languages'), 'url' => ['view']];
$this->params['breadcrumbs'][] = $this->title;
?>
<div class="menu-create">
    <h1><?php echo Html::encode($this->title); ?></h1>
    <?php echo $this->render(
        '_form',
        [
            'model' => $model,
        ]
    ); ?>
</div>
