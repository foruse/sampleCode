<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */

use yii\helpers\Html;
use yii\widgets\ActiveForm;

/* @var yii\web\View $this */
/* @var gbksoft\multilingual\models\Language $model*/
/* @var yii\widgets\ActiveForm $form */

?>
    <div class="form">
        <?php $form = ActiveForm::begin(); ?>
        <?php echo $form->field($model, 'name')->textInput(); ?>
        <?php echo $form->field($model, 'locale')->textInput(); ?>
        <?php echo $form->field($model, 'url')->textInput(); ?>
        <?php echo $form->field($model, 'is_default')->checkbox(); ?>
        <div class="form-group">
            <?php echo Html::submitButton(
                $model->isNewRecord ? Yii::t('language-backend', 'Create') : Yii::t('language-backend', 'Update'),
                [
                    'class' => $model->isNewRecord ? 'btn btn-success' : 'btn btn-primary'
                ]
            ); ?>
        </div>
        <?php ActiveForm::end(); ?>
    </div>
<?php
